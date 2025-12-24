from fastapi import FastAPI, HTTPException, Request, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import Response, StreamingResponse
from pydantic import BaseModel
from typing import Optional, Dict, Any, List
import time
import json
import os
import uuid
import base64
import asyncio

app = FastAPI(title="AirTap Relay Server - Full Proxy")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

DATA_FILE = "/tmp/airtap_data.json"
REQUESTS_FILE = "/tmp/airtap_requests.json"
RESPONSES_FILE = "/tmp/airtap_responses.json"

def load_json(filepath, default):
    try:
        if os.path.exists(filepath):
            with open(filepath, 'r') as f:
                return json.load(f)
    except:
        pass
    return default

def save_json(filepath, data):
    try:
        with open(filepath, 'w') as f:
            json.dump(data, f)
    except:
        pass

def load_data():
    return load_json(DATA_FILE, {"devices": {}, "sessions": {}})

def save_data(data):
    save_json(DATA_FILE, data)

def load_requests():
    return load_json(REQUESTS_FILE, {})

def save_requests(data):
    save_json(REQUESTS_FILE, data)

def load_responses():
    return load_json(RESPONSES_FILE, {})

def save_responses(data):
    save_json(RESPONSES_FILE, data)

# ============ Models ============

class DeviceRegister(BaseModel):
    device_id: str
    email: str
    device_name: str
    local_ip: Optional[str] = None
    port: int = 8080

class ProxyRequest(BaseModel):
    request_id: str
    method: str
    path: str
    headers: Dict[str, str] = {}
    body: Optional[str] = None  # base64 encoded for binary
    params: Dict[str, str] = {}

class ProxyResponse(BaseModel):
    request_id: str
    status_code: int
    headers: Dict[str, str] = {}
    body: str  # base64 encoded
    content_type: str = "application/json"

# ============ Device Registration ============

@app.post("/register")
async def register_device(device: DeviceRegister):
    data = load_data()
    data["devices"][device.device_id] = {
        "device_id": device.device_id,
        "email": device.email,
        "device_name": device.device_name,
        "local_ip": device.local_ip,
        "port": device.port,
        "last_seen": time.time(),
        "online": True
    }
    save_data(data)
    return {"status": "ok", "message": "Device registered"}

@app.post("/heartbeat/{device_id}")
async def heartbeat(device_id: str):
    data = load_data()
    if device_id in data["devices"]:
        data["devices"][device_id]["last_seen"] = time.time()
        data["devices"][device_id]["online"] = True
        save_data(data)
        return {"status": "ok"}
    raise HTTPException(status_code=404, detail="Device not found")

@app.delete("/unregister/{device_id}")
async def unregister_device(device_id: str):
    data = load_data()
    if device_id in data["devices"]:
        data["devices"][device_id]["online"] = False
        save_data(data)
    return {"status": "ok"}

@app.get("/devices/{email}")
async def get_devices_by_email(email: str):
    data = load_data()
    user_devices = []
    for d in data["devices"].values():
        if d["email"] == email:
            is_online = d["online"] and (time.time() - d["last_seen"] < 60)
            user_devices.append({**d, "online": is_online})
    return {"devices": user_devices}

# ============ Session Management ============

@app.post("/session/create")
async def create_session(device_id: str = Form(...), email: str = Form(...)):
    """Desktop creates a session to communicate with a device"""
    data = load_data()
    if device_id not in data["devices"]:
        raise HTTPException(status_code=404, detail="Device not found")
    
    device = data["devices"][device_id]
    if device["email"] != email and email != "thebhikandeshmukh@gmail.com":
        raise HTTPException(status_code=403, detail="Email mismatch")
    
    session_id = str(uuid.uuid4())
    data["sessions"][session_id] = {
        "device_id": device_id,
        "email": email,
        "created": time.time()
    }
    save_data(data)
    return {"session_id": session_id, "device_id": device_id}

# ============ Full Proxy - Desktop Side ============

@app.api_route("/proxy/{device_id}/{path:path}", methods=["GET", "POST", "DELETE", "PUT"])
async def proxy_request(device_id: str, path: str, request: Request):
    """Desktop sends request through relay to phone"""
    data = load_data()
    
    if device_id not in data["devices"]:
        raise HTTPException(status_code=404, detail="Device not found")
    
    device = data["devices"][device_id]
    if not device["online"] or (time.time() - device["last_seen"] > 60):
        raise HTTPException(status_code=503, detail="Device offline")
    
    # Create request for phone to process
    request_id = str(uuid.uuid4())
    body_bytes = await request.body()
    
    req_data = {
        "request_id": request_id,
        "method": request.method,
        "path": "/" + path,
        "headers": dict(request.headers),
        "params": dict(request.query_params),
        "body": base64.b64encode(body_bytes).decode() if body_bytes else None,
        "timestamp": time.time()
    }
    
    # Store request for phone to pick up
    requests = load_requests()
    if device_id not in requests:
        requests[device_id] = []
    requests[device_id].append(req_data)
    save_requests(requests)
    
    # Wait for response (polling with timeout)
    timeout = 30  # seconds
    start = time.time()
    while time.time() - start < timeout:
        responses = load_responses()
        if request_id in responses:
            resp = responses.pop(request_id)
            save_responses(responses)
            
            body = base64.b64decode(resp["body"]) if resp["body"] else b""
            return Response(
                content=body,
                status_code=resp["status_code"],
                headers={k: v for k, v in resp.get("headers", {}).items() if k.lower() not in ["content-length", "transfer-encoding"]},
                media_type=resp.get("content_type", "application/json")
            )
        await asyncio.sleep(0.5)
    
    raise HTTPException(status_code=504, detail="Device timeout")

# ============ Full Proxy - Phone Side ============

@app.get("/proxy/poll/{device_id}")
async def poll_requests(device_id: str):
    """Phone polls for pending requests"""
    requests = load_requests()
    if device_id not in requests:
        return {"requests": []}
    
    pending = requests[device_id]
    requests[device_id] = []
    save_requests(requests)
    return {"requests": pending}

@app.post("/proxy/respond")
async def submit_response(response: ProxyResponse):
    """Phone submits response for a request"""
    responses = load_responses()
    responses[response.request_id] = {
        "status_code": response.status_code,
        "headers": response.headers,
        "body": response.body,
        "content_type": response.content_type
    }
    save_responses(responses)
    return {"status": "ok"}

# ============ File Upload Proxy ============

@app.post("/proxy/{device_id}/upload")
async def proxy_upload(device_id: str, path: str = Form(""), file: UploadFile = File(...)):
    """Handle file uploads through relay"""
    data = load_data()
    
    if device_id not in data["devices"]:
        raise HTTPException(status_code=404, detail="Device not found")
    
    request_id = str(uuid.uuid4())
    file_content = await file.read()
    
    req_data = {
        "request_id": request_id,
        "method": "POST",
        "path": "/api/files/upload",
        "params": {"path": path},
        "headers": {"Content-Type": "multipart/form-data"},
        "body": base64.b64encode(file_content).decode(),
        "filename": file.filename,
        "timestamp": time.time()
    }
    
    requests = load_requests()
    if device_id not in requests:
        requests[device_id] = []
    requests[device_id].append(req_data)
    save_requests(requests)
    
    # Wait for response
    timeout = 60  # longer for uploads
    start = time.time()
    while time.time() - start < timeout:
        responses = load_responses()
        if request_id in responses:
            resp = responses.pop(request_id)
            save_responses(responses)
            body = base64.b64decode(resp["body"]) if resp["body"] else b""
            return Response(content=body, status_code=resp["status_code"])
        await asyncio.sleep(0.5)
    
    raise HTTPException(status_code=504, detail="Upload timeout")

# ============ Health Check ============

@app.get("/")
async def root():
    data = load_data()
    return {
        "service": "AirTap Relay Server (Full Proxy)",
        "status": "running",
        "devices_count": len(data["devices"]),
        "mode": "full_proxy"
    }

@app.get("/health")
async def health():
    return {"status": "healthy"}

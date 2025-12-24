# AirTap Relay Server (Full Proxy)

Signaling + Full Proxy server for AirTap - ALL traffic goes through this server.

## Deploy to Vercel

1. Install Vercel CLI:
```bash
npm i -g vercel
```

2. Deploy:
```bash
cd relay-server
vercel
```

3. Note your URL: `https://your-project.vercel.app`

## How It Works

```
┌──────────┐         ┌─────────────────┐         ┌──────────┐
│  Phone   │ ◄─────► │  Relay Server   │ ◄─────► │ Desktop  │
│  (Ktor)  │  poll   │    (Vercel)     │  proxy  │  Client  │
└──────────┘         └─────────────────┘         └──────────┘
     │                       │                        │
     │    ALL TRAFFIC FLOWS THROUGH RELAY            │
     │  (files, SMS, notifications, screen, etc.)    │
     └───────────────────────────────────────────────┘
```

## API Endpoints

### Device Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Phone registers device |
| POST | `/heartbeat/{device_id}` | Keep device online |
| DELETE | `/unregister/{device_id}` | Device going offline |
| GET | `/devices/{email}` | Get all devices for email |

### Full Proxy (Desktop → Phone)
| Method | Endpoint | Description |
|--------|----------|-------------|
| ANY | `/proxy/{device_id}/{path}` | Proxy any request to phone |
| POST | `/proxy/{device_id}/upload` | File upload through relay |

### Full Proxy (Phone Side)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/proxy/poll/{device_id}` | Phone polls for requests |
| POST | `/proxy/respond` | Phone sends response |

## Flow

1. **Phone** starts server → registers with `/register` → polls `/proxy/poll/{device_id}`
2. **Desktop** selects device → sends request to `/proxy/{device_id}/api/files`
3. **Relay** stores request → Phone picks it up → processes locally → sends response
4. **Relay** returns response to Desktop

## Local Testing

```bash
pip install -r requirements.txt
uvicorn api.index:app --reload --port 8000
```

Open: http://localhost:8000/docs

## Important Notes

- All data flows through Vercel (slower but works across networks)
- Vercel free tier: 100GB bandwidth/month
- For production: Use Redis/database instead of file storage

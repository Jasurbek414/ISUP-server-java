import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import api from '../api/axios'

type Section = 'intro' | 'quickstart' | 'webhook' | 'devices' | 'face' | 'door' | 'events' | 'examples'

const SECTIONS: { id: Section; label: string }[] = [
  { id: 'intro',      label: 'Kirish' },
  { id: 'quickstart', label: 'Tezkor boshlash' },
  { id: 'webhook',    label: 'Webhook' },
  { id: 'devices',    label: 'Qurilma boshqaruv' },
  { id: 'face',       label: 'Yuz yuklash' },
  { id: 'door',       label: 'Eshik nazorati' },
  { id: 'events',     label: 'Eventlar' },
  { id: 'examples',   label: 'Kod misollari' },
]

export default function IntegrationPage() {
  const [activeSection, setActiveSection] = useState<Section>('intro')
  const [activeTab, setActiveTab] = useState<Record<string, string>>({})

  useQuery({
    queryKey: ['integration-docs'],
    queryFn: () => api.get('/integration/docs').then(r => r.data),
    retry: false,
  })

  function setTab(section: string, tab: string) {
    setActiveTab(prev => ({...prev, [section]: tab}))
  }
  function getTab(section: string) {
    return activeTab[section] ?? 'curl'
  }

  const CodeBlock = ({ code }: { code: string }) => (
    <pre className="bg-black/40 border border-white/10 rounded-xl p-4 text-xs text-emerald-300/90 overflow-x-auto font-mono whitespace-pre-wrap">
      {code}
    </pre>
  )

  const TabBar = ({ section, tabs }: { section: string; tabs: string[] }) => (
    <div className="flex gap-1 mb-3">
      {tabs.map(t => (
        <button key={t} onClick={() => setTab(section, t)}
          className={`px-3 py-1 rounded-lg text-xs font-medium transition-all
            ${getTab(section) === t ? 'bg-indigo-500/30 text-indigo-300 border border-indigo-500/40' : 'text-white/40 hover:text-white/70'}`}>
          {t}
        </button>
      ))}
    </div>
  )

  const EndpointRow = ({ method, path, desc }: { method: string; path: string; desc: string }) => {
    const colors: Record<string, string> = {
      GET:    'text-emerald-400 bg-emerald-500/10',
      POST:   'text-blue-400 bg-blue-500/10',
      PUT:    'text-yellow-400 bg-yellow-500/10',
      DELETE: 'text-red-400 bg-red-500/10',
    }
    return (
      <div className="flex items-center gap-3 py-2 border-b border-white/5">
        <span className={`text-xs px-2 py-0.5 rounded font-mono font-bold w-14 text-center ${colors[method] ?? ''}`}>{method}</span>
        <code className="text-white/80 text-xs font-mono flex-1">{path}</code>
        <span className="text-white/40 text-xs">{desc}</span>
      </div>
    )
  }

  const sectionContent: Record<Section, React.ReactNode> = {
    intro: (
      <div className="space-y-6">
        <div>
          <h2 className="text-xl font-bold text-white mb-3">🔗 Integratsiya qo'llanmasi</h2>
          <p className="text-white/70 leading-relaxed">
            ISUP Server sizning platformangiz bilan quyidagi usullar orqali integratsiya bo'ladi:
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            { icon: '📡', title: 'Webhook', desc: "Eventlar avtomatik sizning backendga keladi (POST so'rov)" },
            { icon: '🔑', title: 'REST API', desc: 'X-API-Key header orqali qurilmalarni boshqarish' },
            { icon: '⚡', title: 'Real-time', desc: 'WebSocket orqali live eventlar va qurilma holati' },
          ].map(c => (
            <div key={c.title} className="bg-white/5 border border-white/10 rounded-xl p-4">
              <div className="text-2xl mb-2">{c.icon}</div>
              <h3 className="text-white font-medium mb-1">{c.title}</h3>
              <p className="text-white/50 text-sm">{c.desc}</p>
            </div>
          ))}
        </div>
        <div>
          <h3 className="text-white font-medium mb-3">API Endpoints</h3>
          <div className="bg-white/5 border border-white/10 rounded-xl p-4">
            <EndpointRow method="GET"  path="/api/integration/devices"           desc="Qurilmalar ro'yxati" />
            <EndpointRow method="POST" path="/api/integration/face/enroll"       desc="Yuz yuklash" />
            <EndpointRow method="POST" path="/api/integration/door/open"         desc="Eshik ochish" />
            <EndpointRow method="GET"  path="/api/integration/events"            desc="Eventlar" />
            <EndpointRow method="GET"  path="/api/integration/stream/{deviceId}" desc="RTSP URL" />
          </div>
        </div>
      </div>
    ),

    quickstart: (
      <div className="space-y-6">
        <h2 className="text-xl font-bold text-white mb-3">⚡ Tezkor boshlash</h2>
        <div className="space-y-4">
          <div>
            <h3 className="text-white/80 font-medium mb-2">1. API kalitini oling</h3>
            <p className="text-white/50 text-sm mb-3">Loyiha sozlamalarida Secret Key ni nusxalang.</p>
          </div>
          <div>
            <h3 className="text-white/80 font-medium mb-2">2. Birinchi so'rov</h3>
            <TabBar section="qs" tabs={['curl', 'Python', 'JavaScript']} />
            {getTab('qs') === 'curl' && <CodeBlock code={`curl https://fake-faceid.uzinc.uz/api/integration/devices \\
  -H "X-API-Key: YOUR_SECRET_KEY"`} />}
            {getTab('qs') === 'Python' && <CodeBlock code={`import requests

API_KEY = "YOUR_SECRET_KEY"
BASE_URL = "https://fake-faceid.uzinc.uz/api/integration"

headers = {"X-API-Key": API_KEY}
r = requests.get(f"{BASE_URL}/devices", headers=headers)
print(r.json())`} />}
            {getTab('qs') === 'JavaScript' && <CodeBlock code={`const API_KEY = "YOUR_SECRET_KEY";
const BASE = "https://fake-faceid.uzinc.uz/api/integration";

const res = await fetch(\`\${BASE}/devices\`, {
  headers: { "X-API-Key": API_KEY }
});
const devices = await res.json();
console.log(devices);`} />}
          </div>
        </div>
      </div>
    ),

    webhook: (
      <div className="space-y-6">
        <h2 className="text-xl font-bold text-white mb-3">📡 Webhook integratsiya</h2>
        <div className="bg-indigo-500/10 border border-indigo-500/20 rounded-xl p-4">
          <p className="text-indigo-300 text-sm">Qurilmadan event kelganda ISUP Server sizning Webhook URL ga avtomatik POST so'rov yuboradi.</p>
        </div>
        <div>
          <h3 className="text-white/80 font-medium mb-3">Webhook payload</h3>
          <CodeBlock code={`{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "attendance",
  "device_id": "admin",
  "device_model": "DS-K1T343EWX",
  "employee_no": "EMP001",
  "employee_name": "Ali Valiyev",
  "direction": "in",
  "verify_mode": "face",
  "door_no": 1,
  "event_time": "2024-01-15T09:30:00Z",
  "photo_base64": "...",
  "timestamp": "2024-01-15T09:30:01Z"
}`} />
        </div>
        <div>
          <h3 className="text-white/80 font-medium mb-3">HMAC imzo tekshirish</h3>
          <TabBar section="wh" tabs={['Django', 'Laravel', 'Node.js', 'PHP']} />
          {getTab('wh') === 'Django' && <CodeBlock code={`import hmac, hashlib, json
from django.views.decorators.csrf import csrf_exempt
from django.http import JsonResponse

ISUP_SECRET = "your_secret_key"

@csrf_exempt
def isup_webhook(request):
    sig = request.headers.get('X-ISUP-Signature', '')
    body = request.body
    expected = 'sha256=' + hmac.new(
        ISUP_SECRET.encode(), body, hashlib.sha256
    ).hexdigest()

    if not hmac.compare_digest(sig, expected):
        return JsonResponse({'error': 'Invalid signature'}, status=401)

    event = json.loads(body)
    emp_no = event['employee_no']
    direction = event['direction']  # 'in' yoki 'out'

    # O'z biznes logikangiz
    print(f"{emp_no} {direction} qildi")
    return JsonResponse({'status': 'ok'})`} />}
          {getTab('wh') === 'Laravel' && <CodeBlock code={`<?php
// routes/api.php
Route::post('/isup/webhook', [IsupController::class, 'webhook']);

// app/Http/Controllers/IsupController.php
public function webhook(Request $request)
{
    $secret = config('services.isup.secret');
    $sig = $request->header('X-ISUP-Signature', '');
    $expected = 'sha256=' . hash_hmac('sha256', $request->getContent(), $secret);

    if (!hash_equals($expected, $sig)) {
        return response()->json(['error' => 'Invalid'], 401);
    }

    $data = $request->json()->all();
    $empNo = $data['employee_no'];
    $direction = $data['direction'];

    // Business logic...
    return response()->json(['status' => 'ok']);
}`} />}
          {getTab('wh') === 'Node.js' && <CodeBlock code={`const express = require('express');
const crypto = require('crypto');
const app = express();

const ISUP_SECRET = process.env.ISUP_SECRET;

app.post('/webhook/isup', express.raw({type: '*/*'}), (req, res) => {
  const sig = req.headers['x-isup-signature'] || '';
  const expected = 'sha256=' + crypto
    .createHmac('sha256', ISUP_SECRET)
    .update(req.body)
    .digest('hex');

  if (!crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) {
    return res.status(401).json({ error: 'Invalid signature' });
  }

  const event = JSON.parse(req.body);
  console.log(\`\${event.employee_no} \${event.direction}\`);
  res.json({ status: 'ok' });
});`} />}
          {getTab('wh') === 'PHP' && <CodeBlock code={`<?php
$secret = "your_secret_key";
$sig = $_SERVER['HTTP_X_ISUP_SIGNATURE'] ?? '';
$body = file_get_contents('php://input');

$expected = 'sha256=' . hash_hmac('sha256', $body, $secret);
if (!hash_equals($expected, $sig)) {
    http_response_code(401);
    echo json_encode(['error' => 'Invalid']);
    exit;
}

$event = json_decode($body, true);
$empNo = $event['employee_no'];
// Business logic...
echo json_encode(['status' => 'ok']);`} />}
        </div>
      </div>
    ),

    devices: (
      <div className="space-y-6">
        <h2 className="text-xl font-bold text-white mb-3">📡 Qurilma boshqaruv</h2>
        <div>
          <h3 className="text-white/80 font-medium mb-3">Qurilmalar ro'yxati</h3>
          <CodeBlock code={`GET /api/integration/devices
X-API-Key: your_secret_key

# Javob:
[
  {
    "deviceId": "admin",
    "name": "Kirish eshigi",
    "status": "online",
    "deviceType": "face_terminal",
    "capabilities": ["face", "door", "attendance"]
  }
]`} />
        </div>
      </div>
    ),

    face: (
      <div className="space-y-6">
        <h2 className="text-xl font-bold text-white mb-3">👤 Yuz yuklash</h2>
        <TabBar section="face" tabs={['curl', 'Python', 'JavaScript']} />
        {getTab('face') === 'curl' && <CodeBlock code={`curl -X POST https://fake-faceid.uzinc.uz/api/integration/face/enroll \\
  -H "X-API-Key: YOUR_SECRET_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{
    "employeeNo": "EMP001",
    "name": "Ali Valiyev",
    "gender": "male",
    "photoBase64": "/9j/4AAQSkZJRgAB..."
  }'

# Javob:
{ "status": "success", "devicesUpdated": 2 }`} />}
        {getTab('face') === 'Python' && <CodeBlock code={`import requests, base64

API_KEY = "YOUR_SECRET_KEY"
BASE = "https://fake-faceid.uzinc.uz/api/integration"

with open("photo.jpg", "rb") as f:
    photo_b64 = base64.b64encode(f.read()).decode()

r = requests.post(f"{BASE}/face/enroll",
    headers={"X-API-Key": API_KEY},
    json={
        "employeeNo": "EMP001",
        "name": "Ali Valiyev",
        "gender": "male",
        "photoBase64": photo_b64
    }
)
print(r.json())`} />}
        {getTab('face') === 'JavaScript' && <CodeBlock code={`const API_KEY = "YOUR_SECRET_KEY";
const BASE = "https://fake-faceid.uzinc.uz/api/integration";

async function enrollFace(employeeNo, name, photoFile) {
  const reader = new FileReader();
  reader.readAsDataURL(photoFile);
  const photoB64 = await new Promise(resolve => {
    reader.onload = e => resolve(e.target.result.split(',')[1]);
  });

  const res = await fetch(\`\${BASE}/face/enroll\`, {
    method: 'POST',
    headers: { 'X-API-Key': API_KEY, 'Content-Type': 'application/json' },
    body: JSON.stringify({ employeeNo, name, photoBase64: photoB64 })
  });
  return res.json();
}`} />}
      </div>
    ),

    door: (
      <div className="space-y-6">
        <h2 className="text-xl font-bold text-white mb-3">🚪 Eshik nazorati</h2>
        <CodeBlock code={`# Eshikni ochish
POST /api/integration/door/open
X-API-Key: your_secret_key
Content-Type: application/json

{
  "deviceId": "admin",
  "doorNo": 1,
  "durationSeconds": 5
}

# Javob:
{ "status": "success", "result": "OK" }`} />
      </div>
    ),

    events: (
      <div className="space-y-6">
        <h2 className="text-xl font-bold text-white mb-3">📋 Eventlar</h2>
        <CodeBlock code={`GET /api/integration/events?limit=20&employeeNo=EMP001
X-API-Key: your_secret_key

# Query params:
# from=2024-01-01T00:00:00Z
# to=2024-01-31T23:59:59Z
# employeeNo=EMP001
# limit=20 (default)
# page=0 (default)`} />
      </div>
    ),

    examples: (
      <div className="space-y-6">
        <h2 className="text-xl font-bold text-white mb-3">💡 To'liq misol</h2>
        <TabBar section="ex" tabs={['Django', 'Node.js']} />
        {getTab('ex') === 'Django' && <CodeBlock code={`# Django integratsiya to'liq misol

import requests, base64, hmac, hashlib

class ISUPClient:
    def __init__(self, api_key, base_url="https://fake-faceid.uzinc.uz"):
        self.headers = {"X-API-Key": api_key}
        self.base = f"{base_url}/api/integration"

    def get_devices(self):
        return requests.get(f"{self.base}/devices", headers=self.headers).json()

    def enroll_face(self, employee_no, name, photo_path):
        with open(photo_path, "rb") as f:
            photo_b64 = base64.b64encode(f.read()).decode()
        return requests.post(f"{self.base}/face/enroll",
            headers=self.headers,
            json={"employeeNo": employee_no, "name": name, "photoBase64": photo_b64}
        ).json()

    def open_door(self, device_id, door_no=1, seconds=5):
        return requests.post(f"{self.base}/door/open",
            headers=self.headers,
            json={"deviceId": device_id, "doorNo": door_no, "durationSeconds": seconds}
        ).json()

# Ishlatish:
client = ISUPClient("your_secret_key")
devices = client.get_devices()
result = client.enroll_face("EMP001", "Ali", "photo.jpg")`} />}
        {getTab('ex') === 'Node.js' && <CodeBlock code={`// Node.js integratsiya
const axios = require('axios');
const fs = require('fs');

class ISUPClient {
  constructor(apiKey, baseUrl = 'https://fake-faceid.uzinc.uz') {
    this.client = axios.create({
      baseURL: \`\${baseUrl}/api/integration\`,
      headers: { 'X-API-Key': apiKey }
    });
  }

  async getDevices() { return (await this.client.get('/devices')).data; }

  async enrollFace(employeeNo, name, photoPath) {
    const photo = fs.readFileSync(photoPath).toString('base64');
    return (await this.client.post('/face/enroll', { employeeNo, name, photoBase64: photo })).data;
  }

  async openDoor(deviceId, doorNo = 1, seconds = 5) {
    return (await this.client.post('/door/open', { deviceId, doorNo, durationSeconds: seconds })).data;
  }
}

const client = new ISUPClient('your_secret_key');
const devices = await client.getDevices();`} />}
      </div>
    ),
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-white">Integratsiya</h1>
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4">
        {/* Left nav */}
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-3 space-y-1 h-fit">
          {SECTIONS.map(s => (
            <button key={s.id} onClick={() => setActiveSection(s.id)}
              className={`w-full text-left px-3 py-2 rounded-xl text-sm transition-all
                ${activeSection === s.id ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30' : 'text-white/60 hover:text-white/90 hover:bg-white/5'}`}>
              {s.label}
            </button>
          ))}
        </div>

        {/* Content */}
        <div className="lg:col-span-3 bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-6">
          {sectionContent[activeSection]}
        </div>
      </div>
    </div>
  )
}

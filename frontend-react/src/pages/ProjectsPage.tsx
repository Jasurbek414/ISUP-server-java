import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import api from '../api/axios'
import Modal from '../components/Modal'
import { CardSkeleton } from '../components/Skeleton'
import { toast } from '../components/Toast'

interface Project {
  id: number
  name: string
  webhookUrl: string
  secretKey?: string
  deviceCount?: number
  eventCount?: number
}

interface ProjectForm {
  name: string
  webhookUrl: string
}

export default function ProjectsPage() {
  const [addOpen, setAddOpen] = useState(false)
  const [visibleSecret, setVisibleSecret] = useState<number | null>(null)
  const [integrationProject, setIntegrationProject] = useState<Project | null>(null)
  const qc = useQueryClient()

  const { data: projects, isLoading } = useQuery<Project[]>({
    queryKey: ['projects'],
    queryFn: () => api.get('/projects').then((r) => r.data),
  })

  const { register, handleSubmit, reset } = useForm<ProjectForm>()

  const addMutation = useMutation({
    mutationFn: (data: ProjectForm) => api.post('/projects', data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['projects'] })
      setAddOpen(false)
      reset()
      toast('Loyiha yaratildi', 'success')
    },
    onError: () => toast('Xatolik', 'error'),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/projects/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['projects'] })
      toast("Loyiha o'chirildi", 'success')
    },
    onError: () => toast('Xatolik', 'error'),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Loyihalar</h1>
        <button
          onClick={() => setAddOpen(true)}
          className="px-4 py-2 rounded-xl text-sm bg-indigo-500/20 hover:bg-indigo-500/30
            border border-indigo-500/30 hover:border-indigo-500/50 text-indigo-300
            backdrop-blur-sm transition-all duration-200"
        >
          + Qo'shish
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 3 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : (projects ?? []).length === 0 ? (
        <div className="text-center py-16 text-white/30">
          <div className="text-4xl mb-4">🏢</div>
          <p>Loyihalar topilmadi</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {(projects ?? []).map((p) => (
            <div key={p.id}
              className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-5
                hover:border-white/20 transition-all duration-300">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-2">
                  <span className="text-xl">🏢</span>
                  <h3 className="text-white font-medium">{p.name}</h3>
                </div>
              </div>

              {p.webhookUrl && (
                <p className="text-white/40 text-xs mb-3 truncate">
                  webhook: {p.webhookUrl}
                </p>
              )}

              <div className="flex gap-4 text-xs text-white/50 mb-4">
                <span>{p.deviceCount ?? 0} qurilma</span>
                <span>{p.eventCount ?? 0} event</span>
              </div>

              {p.secretKey && (
                <div className="flex items-center gap-2 mb-4">
                  <span className="text-white/40 text-xs flex-1">
                    Secret: {visibleSecret === p.id ? p.secretKey : '•'.repeat(16)}
                  </span>
                  <button
                    onClick={() => setVisibleSecret(visibleSecret === p.id ? null : p.id)}
                    className="text-white/40 hover:text-white/80 text-xs"
                  >
                    {visibleSecret === p.id ? '🙈' : '👁'}
                  </button>
                </div>
              )}

              <div className="flex gap-2">
                <button
                  onClick={() => setIntegrationProject(p)}
                  className="flex-1 py-2 rounded-xl text-xs bg-indigo-500/20 hover:bg-indigo-500/30
                    border border-indigo-500/30 text-indigo-300 transition-all"
                >
                  🔗 Integratsiya kodi
                </button>
                <button
                  onClick={() => {
                    if (confirm("Loyihani o'chirmoqchimisiz?")) {
                      deleteMutation.mutate(p.id)
                    }
                  }}
                  className="flex-1 py-2 rounded-xl text-xs bg-red-500/20 hover:bg-red-500/30
                    border border-red-500/30 text-red-300 transition-all"
                >
                  O'chirish
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="Loyiha qo'shish">
        <form onSubmit={handleSubmit((d) => addMutation.mutate(d))} className="space-y-4">
          <div>
            <label className="block text-white/60 text-sm mb-2">Loyiha nomi</label>
            <input
              {...register('name', { required: true })}
              placeholder="Mening loyiham"
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50
                focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>
          <div>
            <label className="block text-white/60 text-sm mb-2">Webhook URL</label>
            <input
              {...register('webhookUrl')}
              placeholder="https://yoursite.com/webhook"
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3
                text-white placeholder:text-white/30 focus:outline-none focus:border-indigo-500/50
                focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={() => setAddOpen(false)}
              className="flex-1 py-3 rounded-xl border border-white/10 text-white/60 hover:bg-white/5">
              Bekor
            </button>
            <button type="submit" disabled={addMutation.isPending}
              className="flex-1 py-3 rounded-xl bg-indigo-500/20 hover:bg-indigo-500/30
                border border-indigo-500/30 text-indigo-300 disabled:opacity-50">
              {addMutation.isPending ? '...' : 'Saqlash'}
            </button>
          </div>
        </form>
      </Modal>

      {integrationProject && (
        <IntegrationCodeModal
          project={integrationProject}
          onClose={() => setIntegrationProject(null)}
        />
      )}
    </div>
  )
}

function IntegrationCodeModal({ project, onClose }: { project: Project; onClose: () => void }) {
  const [activeTab, setActiveTab] = useState('Django')
  const [showSecret, setShowSecret] = useState(false)
  const tabs = ['Django', 'Laravel', 'Node.js', 'curl']

  const webhookUrl = project.webhookUrl || 'https://yoursite.com/webhook'
  const secretKey = project.secretKey || 'YOUR_SECRET_KEY'

  const codeExamples: Record<string, string> = {
    Django: `import hmac, hashlib, json
from django.views.decorators.csrf import csrf_exempt
from django.http import JsonResponse

ISUP_SECRET = "${secretKey}"
WEBHOOK_URL = "${webhookUrl}"

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
    # O'z logikangiz...
    return JsonResponse({'status': 'ok'})`,

    Laravel: `<?php
// routes/api.php
Route::post('/isup/webhook', [IsupController::class, 'webhook']);

// IsupController.php
public function webhook(Request $request)
{
    $secret = "${secretKey}";
    $sig = $request->header('X-ISUP-Signature', '');
    $expected = 'sha256=' . hash_hmac('sha256', $request->getContent(), $secret);

    if (!hash_equals($expected, $sig)) {
        return response()->json(['error' => 'Invalid'], 401);
    }

    $data = $request->json()->all();
    // O'z logikangiz...
    return response()->json(['status' => 'ok']);
}`,

    'Node.js': `const express = require('express');
const crypto = require('crypto');
const app = express();

const ISUP_SECRET = "${secretKey}";

app.post('/webhook/isup', express.raw({type: '*/*'}), (req, res) => {
  const sig = req.headers['x-isup-signature'] || '';
  const expected = 'sha256=' + crypto
    .createHmac('sha256', ISUP_SECRET)
    .update(req.body)
    .digest('hex');

  if (!crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) {
    return res.status(401).json({ error: 'Invalid' });
  }

  const event = JSON.parse(req.body);
  // O'z logikangiz...
  res.json({ status: 'ok' });
});`,

    curl: `# Webhook URL ga test so'rov yuborish
curl -X POST "${webhookUrl}" \\
  -H "Content-Type: application/json" \\
  -H "X-ISUP-Signature: sha256=test" \\
  -d '{
    "event_type": "attendance",
    "employee_no": "EMP001",
    "employee_name": "Test Foydalanuvchi",
    "direction": "in",
    "event_time": "2024-01-15T09:30:00Z"
  }'`,
  }

  function copyToClipboard(text: string) {
    navigator.clipboard.writeText(text).then(() => toast('Nusxalandi!', 'success'))
  }

  return (
    <Modal open={true} onClose={onClose} title={`${project.name} — Integratsiya kodi`}>
      <div className="space-y-4">
        {/* Webhook URL */}
        <div>
          <label className="block text-white/50 text-xs mb-1">Webhook URL</label>
          <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl px-4 py-2">
            <code className="text-white/80 text-xs flex-1 truncate">{webhookUrl}</code>
            <button onClick={() => copyToClipboard(webhookUrl)}
              className="text-white/40 hover:text-white/80 text-xs transition-colors">📋</button>
          </div>
        </div>

        {/* Secret Key */}
        {project.secretKey && (
          <div>
            <label className="block text-white/50 text-xs mb-1">Secret Key</label>
            <div className="flex items-center gap-2 bg-white/5 border border-white/10 rounded-xl px-4 py-2">
              <code className="text-white/80 text-xs flex-1">
                {showSecret ? secretKey : '•'.repeat(Math.min(secretKey.length, 32))}
              </code>
              <button onClick={() => setShowSecret(!showSecret)} className="text-white/40 hover:text-white/80 text-xs">{showSecret ? '🙈' : '👁'}</button>
              <button onClick={() => copyToClipboard(secretKey)} className="text-white/40 hover:text-white/80 text-xs">📋</button>
            </div>
          </div>
        )}

        {/* Code tabs */}
        <div>
          <div className="flex gap-1 mb-3">
            {tabs.map(t => (
              <button key={t} onClick={() => setActiveTab(t)}
                className={`px-3 py-1 rounded-lg text-xs font-medium transition-all
                  ${activeTab === t ? 'bg-indigo-500/30 text-indigo-300 border border-indigo-500/40' : 'text-white/40 hover:text-white/70'}`}>
                {t}
              </button>
            ))}
          </div>
          <div className="relative">
            <pre className="bg-black/40 border border-white/10 rounded-xl p-4 text-xs text-emerald-300/90 overflow-x-auto font-mono whitespace-pre-wrap max-h-64 overflow-y-auto">
              {codeExamples[activeTab]}
            </pre>
            <button onClick={() => copyToClipboard(codeExamples[activeTab])}
              className="absolute top-2 right-2 px-2 py-1 rounded-lg text-xs bg-white/10 hover:bg-white/20 text-white/60 hover:text-white/90 transition-all">
              📋 Nusxalash
            </button>
          </div>
        </div>

        <button onClick={onClose}
          className="w-full py-3 rounded-xl border border-white/10 text-white/60 hover:bg-white/5 transition-all">
          Yopish
        </button>
      </div>
    </Modal>
  )
}

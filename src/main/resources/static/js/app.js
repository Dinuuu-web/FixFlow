// FixFlow — Main JavaScript
'use strict';

// ============================================================
// TOAST NOTIFICATIONS
// ============================================================
window.showToast = function(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div class="toast-bar"></div>
    <span>${escapeHtml(message)}</span>
    <button class="toast-close" onclick="this.parentElement.remove()">×</button>
  `;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = 'fadeOut 0.3s ease forwards';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
};

// Auto-show flash messages as toasts
document.addEventListener('DOMContentLoaded', function() {
  const success = document.querySelector('[data-toast-success]');
  const error   = document.querySelector('[data-toast-error]');
  if (success) showToast(success.dataset.toastSuccess, 'success');
  if (error)   showToast(error.dataset.toastError,   'error');
});

// ============================================================
// HTML ESCAPE (security)
// ============================================================
function escapeHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(str));
  return div.innerHTML;
}

// ============================================================
// SIDEBAR TOGGLE (mobile)
// ============================================================
document.addEventListener('DOMContentLoaded', function() {
  const toggleBtn = document.getElementById('sidebar-toggle');
  const sidebar   = document.getElementById('sidebar');
  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener('click', () => {
      sidebar.classList.toggle('open');
    });
    document.addEventListener('click', (e) => {
      if (!sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
        sidebar.classList.remove('open');
      }
    });
  }
});

// ============================================================
// CONFIRM DIALOG
// ============================================================
window.confirmAction = function(message, formId) {
  if (confirm(message)) {
    document.getElementById(formId).submit();
  }
};

// ============================================================
// AUTO-DISMISS ALERTS
// ============================================================
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('.alert[data-auto-dismiss]').forEach(alert => {
    setTimeout(() => {
      alert.style.opacity = '0';
      alert.style.transition = 'opacity 0.5s';
      setTimeout(() => alert.remove(), 500);
    }, 5000);
  });
});

// ============================================================
// PASSWORD SHOW/HIDE TOGGLE
// ============================================================
document.addEventListener('DOMContentLoaded', function() {
  const toggleBtn   = document.getElementById('togglePassword');
  const passwordIn  = document.getElementById('password');
  const eyeOpen     = document.getElementById('eyeOpen');
  const eyeClosed   = document.getElementById('eyeClosed');

  if (toggleBtn && passwordIn && eyeOpen && eyeClosed) {
    toggleBtn.addEventListener('click', function() {
      const isPassword = passwordIn.type === 'password';
      passwordIn.type = isPassword ? 'text' : 'password';
      eyeOpen.style.display   = isPassword ? 'none'  : 'block';
      eyeClosed.style.display = isPassword ? 'block' : 'none';
      toggleBtn.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');
      passwordIn.focus();
    });
  }
});

// ============================================================
// FILE INPUT PREVIEW
// ============================================================
document.addEventListener('DOMContentLoaded', function() {
  const fileInput = document.getElementById('imageUpload');
  const preview   = document.getElementById('imagePreview');
  if (fileInput && preview) {
    fileInput.addEventListener('change', function() {
      const file = this.files[0];
      if (!file) { preview.style.display = 'none'; return; }

      // Validate type
      const allowed = ['image/jpeg','image/png','image/gif','image/webp'];
      if (!allowed.includes(file.type)) {
        showToast('Only images (JPG, PNG, GIF, WEBP) are allowed.', 'error');
        this.value = '';
        preview.style.display = 'none';
        return;
      }
      // Validate size (5MB)
      if (file.size > 5 * 1024 * 1024) {
        showToast('File size must not exceed 5MB.', 'error');
        this.value = '';
        preview.style.display = 'none';
        return;
      }

      const reader = new FileReader();
      reader.onload = e => {
        preview.src = e.target.result;
        preview.style.display = 'block';
      };
      reader.readAsDataURL(file);
    });
  }
});

// ============================================================
// CHARACTER COUNTER
// ============================================================
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('[data-max-chars]').forEach(el => {
    const max = parseInt(el.dataset.maxChars);
    const counterId = el.dataset.counter;
    const counter = document.getElementById(counterId);
    if (!counter) return;
    el.addEventListener('input', () => {
      const remaining = max - el.value.length;
      counter.textContent = remaining + ' chars remaining';
      counter.style.color = remaining < 50 ? '#ef4444' : '';
    });
  });
});

// ============================================================
// AI ASSISTANT
// ============================================================
const AI = {
  currentTab: 'improve',

  init() {
    document.querySelectorAll('.ai-tab').forEach(tab => {
      tab.addEventListener('click', () => this.switchTab(tab.dataset.tab));
    });

    document.querySelectorAll('[data-ai-action]').forEach(btn => {
      btn.addEventListener('click', () => this.runAction(btn.dataset.aiAction));
    });
  },

  switchTab(tabId) {
    document.querySelectorAll('.ai-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.ai-tab-content').forEach(c => c.classList.remove('active'));
    document.querySelector(`.ai-tab[data-tab="${tabId}"]`)?.classList.add('active');
    document.getElementById(`tab-${tabId}`)?.classList.add('active');
    this.currentTab = tabId;
  },

  async runAction(action) {
    const contentEl  = document.getElementById(`ai-input-${action}`);
    const resultEl   = document.getElementById(`ai-result-${action}`);
    const btnEl      = document.querySelector(`[data-ai-action="${action}"]`);
    const categoryEl = document.getElementById('ai-category-hint');
    const complaintEl = document.getElementById('ai-complaint-id');

    if (!contentEl || !resultEl) return;

    const content = contentEl.value.trim();
    if (!content) {
      showToast('Please enter some text first.', 'error');
      return;
    }

    resultEl.innerHTML = '<div class="ai-loading"><div class="spinner"></div><span>Thinking...</span></div>';
    if (btnEl) btnEl.disabled = true;

    const body = {
      action,
      content,
      category: categoryEl?.value || '',
      complaintId: complaintEl?.value || ''
    };

    try {
      const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
      const headers = {'Content-Type': 'application/json'};
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

      const resp = await fetch('/ai/api/assist', {
        method: 'POST',
        headers,
        body: JSON.stringify(body)
      });

      if (!resp.ok) throw new Error('Request failed: ' + resp.status);
      const data = await resp.json();

      if (data.error) {
        resultEl.innerHTML = `<span style="color:var(--danger)">⚠ ${escapeHtml(data.error)}</span>`;
      } else {
        resultEl.textContent = data.result || 'No response received.';
      }
    } catch (err) {
      resultEl.innerHTML = `<span style="color:var(--danger)">⚠ Failed to connect to AI service. Please try again.</span>`;
    } finally {
      if (btnEl) btnEl.disabled = false;
    }
  },

  copyResult(action) {
    const el = document.getElementById(`ai-result-${action}`);
    if (el) {
      navigator.clipboard.writeText(el.textContent)
        .then(() => showToast('Copied to clipboard!'))
        .catch(() => showToast('Copy failed.', 'error'));
    }
  }
};

document.addEventListener('DOMContentLoaded', () => {
  if (document.querySelector('.ai-tabs')) {
    AI.init();
  }
});

// ============================================================
// CHARTS — Dashboard
// ============================================================
window.initDashboardCharts = function(categoryData, statusData) {
  // Category Chart
  const catCtx = document.getElementById('categoryChart');
  if (catCtx && categoryData && categoryData.length > 0) {
    const labels = categoryData.map(d => d[0]);
    const values = categoryData.map(d => d[1]);
    new Chart(catCtx, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: [
            '#2563eb','#0ea5e9','#8b5cf6','#10b981','#f59e0b',
            '#ef4444','#6366f1','#ec4899','#14b8a6','#f97316'
          ],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 12, font: { size: 12 } } }
        },
        cutout: '65%'
      }
    });
  }

  // Status Chart
  const statusCtx = document.getElementById('statusChart');
  if (statusCtx && statusData && statusData.length > 0) {
    const statusColors = {
      'PENDING':     '#f59e0b',
      'ASSIGNED':    '#3b82f6',
      'IN_PROGRESS': '#8b5cf6',
      'RESOLVED':    '#10b981',
      'REJECTED':    '#ef4444',
      'CANCELLED':   '#94a3b8'
    };
    const labels = statusData.map(d => d[0]);
    const values = statusData.map(d => d[1]);
    const colors = labels.map(l => statusColors[l] || '#94a3b8');

    new Chart(statusCtx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: colors,
          borderRadius: 6,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: { legend: { display: false } },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { stepSize: 1, font: { size: 11 } },
            grid: { color: '#f1f5f9' }
          },
          x: { ticks: { font: { size: 11 } }, grid: { display: false } }
        }
      }
    });
  }
};

// ============================================================
// FILTERS — Quick filter via URL
// ============================================================
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('[data-filter-select]').forEach(sel => {
    sel.addEventListener('change', function() {
      const url = new URL(window.location.href);
      if (this.value) {
        url.searchParams.set(this.dataset.filterSelect, this.value);
      } else {
        url.searchParams.delete(this.dataset.filterSelect);
      }
      window.location.href = url.toString();
    });
  });
});

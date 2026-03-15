// ============================================
//  Two-Factor Auth — Shared JavaScript Utils
// ============================================

/**
 * Show an alert message
 */
function showAlert(id, message, type) {
    const alert = document.getElementById(id);
    if (!alert) return;

    alert.className = `alert show alert-${type}`;
    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    alert.querySelector('.alert-icon').textContent = icon;
    alert.querySelector('.alert-msg').textContent = message;
}

/**
 * Hide an alert message
 */
function hideAlert(id) {
    const alert = document.getElementById(id);
    if (!alert) return;
    alert.classList.remove('show');
}

/**
 * Toggle password visibility
 */
function togglePassword(inputId, btn) {
    const input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
        btn.textContent = 'hide';
    } else {
        input.type = 'password';
        btn.textContent = 'show';
    }
}

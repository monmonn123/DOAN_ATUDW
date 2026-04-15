// EDUMOET - Main JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Vote buttons handler
    const voteButtons = document.querySelectorAll('.vote-btn');
    
    voteButtons.forEach(button => {
        button.addEventListener('click', async function(e) {
            e.preventDefault();
            
            const id = this.getAttribute('data-id');
            const type = this.getAttribute('data-type');
            const action = this.getAttribute('data-action');
            
            try {
                const response = await fetch(`/${type}s/${id}/${action}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });
                
                if (response.ok) {
                    const votes = await response.text();
                    // Update vote count display
                    const voteDisplay = this.parentElement.querySelector('.h4');
                    if (voteDisplay) {
                        voteDisplay.textContent = votes;
                    }
                    
                    // Visual feedback
                    this.classList.add('btn-primary');
                    setTimeout(() => {
                        this.classList.remove('btn-primary');
                    }, 500);
                } else {
                    alert('Error voting. Please try again.');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('Error voting. Please try again.');
            }
        });
    });
    
    // Auto-hide alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.opacity = '0';
            setTimeout(() => {
                alert.remove();
            }, 300);
        }, 5000);
    });
    
    // Confirm delete actions
    const deleteButtons = document.querySelectorAll('[data-confirm]');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            const message = this.getAttribute('data-confirm');
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });
    
    // Tag input helper
    const tagInput = document.getElementById('tags');
    if (tagInput) {
        tagInput.addEventListener('input', function() {
            // Limit to 5 tags
            const tags = this.value.trim().split(/\s+/);
            if (tags.length > 5) {
                this.value = tags.slice(0, 5).join(' ');
            }
        });
    }
    
    // Character counter for textareas
    const textareas = document.querySelectorAll('textarea[maxlength]');
    textareas.forEach(textarea => {
        const maxLength = textarea.getAttribute('maxlength');
        if (maxLength) {
            const counter = document.createElement('small');
            counter.className = 'form-text text-muted';
            counter.textContent = `0 / ${maxLength} characters`;
            textarea.parentElement.appendChild(counter);
            
            textarea.addEventListener('input', function() {
                counter.textContent = `${this.value.length} / ${maxLength} characters`;
            });
        }
    });
    
    // Search input focus
    const searchInput = document.querySelector('input[name="q"]');
    if (searchInput) {
        // Ctrl+K or Cmd+K to focus search
        document.addEventListener('keydown', function(e) {
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                searchInput.focus();
            }
        });
    }
});

// Utility function to format date
function formatDate(dateString) {
    const date = new Date(dateString);
    const options = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return date.toLocaleDateString('en-US', options);
}

// Utility function to escape HTML
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}


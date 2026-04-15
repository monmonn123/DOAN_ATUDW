// Image handling functions
function showImage(imageId) {
    const modal = document.getElementById('imageModal');
    const modalImg = document.getElementById('fullImage');
    modal.style.display = 'block';
    modalImg.src = `/api/images/${imageId}`;
}

function closeImageModal() {
    const modal = document.getElementById('imageModal');
    modal.style.display = 'none';
}

// Close modal when clicking outside the image
document.addEventListener('click', function(event) {
    const modal = document.getElementById('imageModal');
    const modalImg = document.getElementById('fullImage');
    if (event.target !== modalImg && modal.style.display === 'block') {
        closeImageModal();
    }
});

// Preview images before upload
function previewImages(event, previewContainerId) {
    const files = event.target.files;
    const previewContainer = document.getElementById(previewContainerId);
    
    if (!previewContainer) return;
    
    previewContainer.innerHTML = ''; // Clear existing previews
    
    if (files.length > 0) {
        previewContainer.style.display = 'block';
        
        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            
            // Check if file is an image
            if (!file.type.match('image.*')) {
                continue;
            }
            
            const reader = new FileReader();
            
            reader.onload = function(e) {
                const img = document.createElement('img');
                img.src = e.target.result;
                img.className = 'img-thumbnail me-2 mb-2';
                img.style.maxWidth = '150px';
                img.style.maxHeight = '150px';
                previewContainer.appendChild(img);
            };
            
            reader.readAsDataURL(file);
        }
    } else {
        previewContainer.style.display = 'none';
    }
}
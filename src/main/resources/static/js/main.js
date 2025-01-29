document.getElementById('uploadForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = new FormData();
    const imageFile = document.getElementById('image').files[0];
    const productName = document.getElementById('productName').value;
    const detailMessage = document.getElementById('detailMessage').value;

    if (!imageFile || !productName || !detailMessage) {
        alert('모든 필드를 입력해주세요.');
        return;
    }

    formData.append('image', imageFile);
    formData.append('productName', productName);
    formData.append('detailMessage', detailMessage);
    
    try {
        const response = await fetch('/api/gifticon', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            throw new Error('서버 오류가 발생했습니다.');
        }

        const data = await response.json();
        console.log('Server Response:', data);
        
        // 결과 페이지로 이동
        window.location.href = '/share/' + data.shareCode;
    } catch (error) {
        console.error('Error:', error);
        alert('업로드 중 오류가 발생했습니다: ' + error.message);
    }
});

function shareKakao() {
    if (!Kakao.isInitialized()) {
        Kakao.init('YOUR_KAKAO_APP_KEY');
    }

    const productName = document.getElementById('resultProductName').textContent;
    const detailMessage = document.getElementById('resultDetailMessage').textContent;
    const qrCodeUrl = document.getElementById('qrCode').src;

    Kakao.Link.sendDefault({
        objectType: 'feed',
        content: {
            title: productName,
            description: detailMessage,
            imageUrl: qrCodeUrl,
            link: {
                mobileWebUrl: window.location.href,
                webUrl: window.location.href,
            },
        },
        buttons: [
            {
                title: '자세히 보기',
                link: {
                    mobileWebUrl: window.location.href + '/share/' + document.getElementById('result').dataset.shareCode,
                    webUrl: window.location.href + '/share/' + document.getElementById('result').dataset.shareCode,
                },
            },
        ],
    });
}

function copyShareLink() {
    const shareUrl = window.location.origin + '/share/' + document.getElementById('result').dataset.shareCode;
    navigator.clipboard.writeText(shareUrl)
        .then(() => alert('링크가 복사되었습니다.'))
        .catch(() => alert('링크 복사에 실패했습니다.'));
} 
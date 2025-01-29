document.getElementById('uploadForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    console.log('Form submitted'); // 디버깅용 로그
    
    const formData = new FormData();
    const imageFile = document.getElementById('image').files[0];
    const productName = document.getElementById('productName').value;
    const detailMessage = document.getElementById('detailMessage').value;

    console.log('Image:', imageFile); // 디버깅용 로그
    console.log('Product Name:', productName); // 디버깅용 로그
    console.log('Detail Message:', detailMessage); // 디버깅용 로그

    formData.append('image', imageFile);
    formData.append('productName', productName);
    formData.append('detailMessage', detailMessage);
    
    try {
        const response = await fetch('/api/gifticon', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            const errorData = await response.text();
            console.error('Server Error:', errorData); // 디버깅용 로그
            throw new Error(errorData);
        }

        const data = await response.json();
        console.log('Server Response:', data); // 디버깅용 로그
        
        document.getElementById('resultProductName').textContent = productName;
        document.getElementById('resultDetailMessage').textContent = detailMessage;
        document.getElementById('qrCode').src = data.qrCodeUrl;
        document.getElementById('barcode').src = data.barcodeUrl;
        document.getElementById('result').style.display = 'block';
        document.getElementById('result').dataset.shareCode = data.shareCode;
    } catch (error) {
        console.error('Error:', error); // 디버깅용 로그
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
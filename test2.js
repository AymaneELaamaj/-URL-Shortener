import http from 'k6/http';
import { sleep, check } from 'k6';

export let options = {
    vus: 20,
    duration: '1m',
};

// Générer des codes de 6-8 caractères
function generateValidCode() {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    const length = 6 + Math.floor(Math.random() * 3); // 6, 7 ou 8 caractères
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

export default function () {
    const randomCode = generateValidCode();

    const createRes = http.post(
        'http://localhost:8081/api/url/create',
        JSON.stringify({
            shortCode: randomCode,
            originalUrl: `https://example.com/${randomCode}`
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    check(createRes, {
        'create status 200': (r) => r.status === 200,
        'not validation error': (r) => r.status !== 400,
    });

    sleep(0.5);
}
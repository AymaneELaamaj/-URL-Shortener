import http from 'k6/http';
import { sleep, check } from 'k6';

export let options = {
    stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<50'], // Plus strict car cache hit
        http_req_failed: ['rate<0.001'], // Presque 0 erreurs
    },
};

export default function () {
    // Configurer pour suivre les redirections et accepter 302
    let params = {
        redirects: 0, // Ne pas suivre les redirections automatiquement
        timeout: '30s'
    };

    let res = http.get('http://localhost:8081/api/url/google', params);

    // Vérifier le status 302 (redirection)
    check(res, {
        'status is 302': (r) => r.status === 302,
        'has location header': (r) => r.headers['Location'] !== undefined,
        'location is google.com': (r) => r.headers['Location'] === 'https://www.google.com',
        'response time < 50ms': (r) => r.timings.duration < 50,
    });

    sleep(0.1);
}
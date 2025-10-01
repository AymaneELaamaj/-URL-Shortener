import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    vus: 1,
    iterations: 150,
};

export default function () {
    const params = {
        redirects: 0,
        timeout: '5s',
    };

    let res = http.get('http://localhost:8081/api/url/google', params);

    if (res.status === 429) {
        console.log(`🚨 BLOCKED! Request ${__ITER}: Status=429`);
    } else if (res.status === 302) {
        if (__ITER <= 100 || __ITER % 10 === 0) {
            console.log(`✅ ALLOWED - Request ${__ITER}: Status=302`);
        }
    }

    sleep(0.05);
}
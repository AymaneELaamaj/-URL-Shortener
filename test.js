import http from 'k6/http';
import { sleep, check } from 'k6';

export let options = {
    vus: 50,         // virtual users simultanés
    duration: '60s', // durée totale du test
    thresholds: {
        http_req_duration: ['p(95)<200'], // 95% des requêtes < 200ms
    },
};

export default function () {
    let res = http.get('http://localhost:8081/api/url/google12');

    // Vérifier que la réponse est OK
    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(0.05); // léger délai entre les requêtes
}

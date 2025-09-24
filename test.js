import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    vus: 20, // virtual users
    duration: '20s',
};

export default function () {
    http.get('http://localhost:8081/api/url/abc123');
    sleep(0.1);
}

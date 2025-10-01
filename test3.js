const hotKeys = ['google', 'github', 'stack', 'blog', 'test'];

export default function () {
    const key = hotKeys[Math.floor(Math.random() * hotKeys.length)];
    const params = { redirects: 0, timeout: '10s' };

    let res = http.get(`http://localhost:8081/api/url/${key}`, params);

    check(res, {
        'status is 302': (r) => r.status === 302,
        'response time < 30ms': (r) => r.timings.duration < 30,
    });

    sleep(0.05);
}
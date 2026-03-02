import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '60s',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01']
  }
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const url = `${BASE_URL}/api/v1/answer?date=2025-11-11&sortBy=rateToRub&direction=desc`;
  const res = http.get(url);
  check(res, {
    'status is 200': (r) => r.status === 200
  });
  sleep(1);
}

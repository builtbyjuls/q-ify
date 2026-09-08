import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ProbeApi, ProbeResponse } from './probe-api.service';

describe('ProbeApi', () => {
  let api: ProbeApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    api = TestBed.inject(ProbeApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the relative probe URL', () => {
    let result: ProbeResponse | undefined;

    api.getStatus().subscribe((response) => (result = response));

    const request = http.expectOne('/api/probe');
    expect(request.request.method).toBe('GET');
    expect(request.request.urlWithParams).toBe('/api/probe');

    request.flush({ status: 'ready' });
    expect(result).toEqual({ status: 'ready' });
  });
});

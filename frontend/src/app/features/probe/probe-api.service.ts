import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProbeResponse {
  readonly status: 'ready';
}

@Injectable({ providedIn: 'root' })
export class ProbeApi {
  private readonly http = inject(HttpClient);

  getStatus(): Observable<ProbeResponse> {
    return this.http.get<ProbeResponse>('/api/probe');
  }
}

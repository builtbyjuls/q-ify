import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { Subject } from 'rxjs';

import { ProbeApi, ProbeResponse } from './probe-api.service';
import { ProbeComponent } from './probe.component';

describe('ProbeComponent', () => {
  it('renders failure and reconnecting states, then recovers on retry', async () => {
    const requests: Subject<ProbeResponse>[] = [];
    const api = {
      getStatus: vi.fn(() => {
        const request = new Subject<ProbeResponse>();
        requests.push(request);
        return request.asObservable();
      })
    };

    await TestBed.configureTestingModule({
      imports: [ProbeComponent],
      providers: [{ provide: ProbeApi, useValue: api }]
    }).compileComponents();

    const fixture = TestBed.createComponent(ProbeComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-state="loading"]')).not.toBeNull();

    requests[0].error(new Error('unavailable'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-state="error"]')?.textContent).toContain(
      'The service could not be reached.'
    );

    const loader = TestbedHarnessEnvironment.loader(fixture);
    const retryButton = await loader.getHarness(MatButtonHarness.with({ text: 'Retry' }));
    await retryButton.click();
    fixture.detectChanges();
    expect(api.getStatus).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('[data-state="reconnecting"]')?.textContent).toContain(
      'Reconnecting to the service...'
    );

    requests[1].next({ status: 'ready' });
    requests[1].complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-state="ready"]')?.textContent).toContain(
      'The service is ready.'
    );
  });
});

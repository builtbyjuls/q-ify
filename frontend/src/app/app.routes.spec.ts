import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from './app.routes';
import { ProbeComponent } from './features/probe/probe.component';

describe('application routes', () => {
  it('lazy-loads the deep probe route', async () => {
    const statusRoute = routes.find((route) => route.path === 'status');
    expect(statusRoute?.loadChildren).toBeTypeOf('function');
    expect(statusRoute?.component).toBeUndefined();

    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/status/probe', ProbeComponent);
    expect(component).toBeInstanceOf(ProbeComponent);

    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/probe').flush({ status: 'ready' });
    await harness.fixture.whenStable();
    expect(harness.routeNativeElement?.textContent).toContain('The service is ready.');
    http.verify();
  });
});

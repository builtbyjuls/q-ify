import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ProbeApi } from './probe-api.service';

type ProbePhase = 'loading' | 'ready' | 'error' | 'reconnecting';

@Component({
  selector: 'app-probe',
  imports: [MatButtonModule, MatCardModule, MatProgressSpinnerModule],
  templateUrl: './probe.component.html',
  styleUrl: './probe.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProbeComponent {
  private readonly api = inject(ProbeApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly phase = signal<ProbePhase>('loading');
  protected readonly isBusy = computed(() => {
    const phase = this.phase();
    return phase === 'loading' || phase === 'reconnecting';
  });
  protected readonly message = computed(() => {
    switch (this.phase()) {
      case 'loading':
        return 'Checking the service...';
      case 'ready':
        return 'The service is ready.';
      case 'error':
        return 'The service could not be reached.';
      case 'reconnecting':
        return 'Reconnecting to the service...';
    }
  });

  constructor() {
    this.load(false);
  }

  protected retry(): void {
    this.load(true);
  }

  private load(isRetry: boolean): void {
    this.phase.set(isRetry ? 'reconnecting' : 'loading');

    this.api
      .getStatus()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.phase.set('ready'),
        error: () => this.phase.set('error')
      });
  }
}

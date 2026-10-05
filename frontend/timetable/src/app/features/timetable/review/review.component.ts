import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, interval, switchMap, takeWhile } from 'rxjs';

import { TimetableApiService } from '../../../core/services/timetable-api.service';

import {
  AiTimetableEntry,
  ExtractionJobStatusResponse,
  TimetableValidationIssue
} from '../../../core/models/timetable.models';

@Component({
  selector: 'app-review',
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './review.component.html',
  styleUrl: './review.component.css'
})
export class ReviewComponent implements OnInit, OnDestroy {

  private readonly api = inject(TimetableApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private pollingSubscription?: Subscription;

  jobId = '';

  status: ExtractionJobStatusResponse | null = null;

  entries: AiTimetableEntry[] = [];

  isConfirming = false;

  errorMessage: string | null = null;

  ngOnInit(): void {

    this.jobId =
      this.route.snapshot.paramMap.get('jobId') ?? '';

    if (!this.jobId) {

      this.errorMessage =
        'No extraction job was provided.';

      return;
    }

    this.startPolling();
  }

  ngOnDestroy(): void {

    this.pollingSubscription?.unsubscribe();
  }

  private startPolling(): void {

    this.pollingSubscription = interval(2000)
      .pipe(
        switchMap(() =>
          this.api.getJobStatus(this.jobId)
        ),

        takeWhile(
          response =>
            response.status === 'PROCESSING',
          true
        )
      )
      .subscribe({

        next: response => {

          this.status = response;

          if (response.status === 'NEEDS_CONFIRMATION') {

            this.entries =
              response.result?.entries
                ? structuredClone(response.result.entries)
                : [];

            this.pollingSubscription?.unsubscribe();

          }

          if (response.status === 'COMPLETED') {

            this.pollingSubscription?.unsubscribe();

          }

          if (response.status === 'FAILED') {

            this.errorMessage =
              response.error ??
              'Timetable extraction failed.';

            this.pollingSubscription?.unsubscribe();

          }
        },

        error: error => {

          console.error(error);

          this.errorMessage =
            'Unable to retrieve the extraction status.';

          this.pollingSubscription?.unsubscribe();
        }

      });
  }

  get validationIssues(): TimetableValidationIssue[] {

    return this.status?.validationIssues ?? [];
  }

  get errors(): TimetableValidationIssue[] {

    return this.validationIssues.filter(
      issue => issue.severity === 'ERROR'
    );
  }

  get warnings(): TimetableValidationIssue[] {

    return this.validationIssues.filter(
      issue => issue.severity === 'WARNING'
    );
  }

  hasIssueForEntry(index: number): boolean {

    return this.validationIssues.some(
      issue => issue.entryIndex === index
    );
  }

  getIssuesForEntry(
    index: number
  ): TimetableValidationIssue[] {

    return this.validationIssues.filter(
      issue => issue.entryIndex === index
    );
  }

  removeEntry(index: number): void {

    this.entries.splice(index, 1);

    this.recalculateIssueIndexes();
  }

  addEntry(): void {

    this.entries.push({
      day: 'Lundi',
      start_time: '08:00',
      end_time: '09:30',
      subject: '',
      teacher: '',
      room: '',
      group: null,
      type: 'cours',
      uncertainties: []
    });
  }

  private recalculateIssueIndexes(): void {

    // Validation will be performed again by the backend
    // when the user confirms the timetable.

    if (this.status) {

      this.status = {
        ...this.status,
        validationIssues: []
      };
    }
  }

  confirm(): void {

    if (this.isConfirming) {
      return;
    }

    this.errorMessage = null;
    this.isConfirming = true;

    this.api
      .confirmTimetable(
        this.jobId,
        {
          entries: this.entries,
          questions_for_user: []
        }
      )
      .subscribe({

        next: response => {

          this.status = response;

          this.isConfirming = false;

          if (response.status === 'COMPLETED') {
            return;
          }

          if (response.status === 'NEEDS_CONFIRMATION') {

            this.entries =
              response.result?.entries
                ? structuredClone(response.result.entries)
                : [];

            return;
          }
        },

        error: error => {

          console.error(error);

          this.errorMessage =
            error?.error?.message ??
            'Unable to confirm the timetable.';

          this.isConfirming = false;
        }

      });
  }

  downloadCalendar(): void {

    window.location.href =
      this.api.getCalendarUrl(this.jobId);
  }

  backToUpload(): void {

    this.router.navigate(['/upload']);
  }
}

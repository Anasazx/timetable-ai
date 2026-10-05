import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  AiTimetableResponse,
  ExtractionJobResponse,
  ExtractionJobStatusResponse
} from '../models/timetable.models';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TimetableApiService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/timetables';

  extractTimetable(
    image: File,
    semesterStart: string,
    semesterEnd: string
  ): Observable<ExtractionJobResponse> {

    const formData = new FormData();

    formData.append('image', image);
    formData.append('semesterStart', semesterStart);
    formData.append('semesterEnd', semesterEnd);

    return this.http.post<ExtractionJobResponse>(
      `${this.apiUrl}/extract`,
      formData
    );
  }

  getJobStatus(
    jobId: string
  ): Observable<ExtractionJobStatusResponse> {

    return this.http.get<ExtractionJobStatusResponse>(
      `${this.apiUrl}/jobs/${jobId}`
    );
  }

  confirmTimetable(
    jobId: string,
    timetable: AiTimetableResponse
  ): Observable<ExtractionJobStatusResponse> {

    return this.http.post<ExtractionJobStatusResponse>(
      `${this.apiUrl}/jobs/${jobId}/confirm`,
      timetable
    );
  }

  getCalendarUrl(jobId: string): string {

    return `${this.apiUrl}/jobs/${jobId}/calendar`;
  }
}

export interface AiTimetableEntry {
  day: string;
  start_time: string;
  end_time: string;
  subject: string;
  teacher: string;
  room: string;
  group: string | null;
  type: string | null;
  uncertainties: string[];
}

export interface AiTimetableResponse {
  entries: AiTimetableEntry[];
  questions_for_user: string[];
}

export interface TimetableValidationIssue {
  entryIndex: number;
  type: string;
  severity: 'ERROR' | 'WARNING';
  message: string;
}

export type ExtractionJobStatus =
  | 'PROCESSING'
  | 'COMPLETED'
  | 'NEEDS_CONFIRMATION'
  | 'FAILED';

export interface ExtractionJobResponse {
  jobId: string;
  status: ExtractionJobStatus;
}

export interface ExtractionJobStatusResponse {
  jobId: string;
  status: ExtractionJobStatus;
  semesterStart: string;
  semesterEnd: string;
  result: AiTimetableResponse | null;
  validationIssues: TimetableValidationIssue[];
  error: string | null;
}

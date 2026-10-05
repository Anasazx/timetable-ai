import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TimetableApiService } from '../../../core/services/timetable-api.service';

@Component({
  selector: 'app-upload',
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './upload.component.html',
  styleUrl: './upload.component.css'
})
export class UploadComponent {

  private readonly api = inject(TimetableApiService);
  private readonly router = inject(Router);

  selectedFile: File | null = null;
  previewUrl: string | null = null;

  semesterStart = '2026-09-15';
  semesterEnd = '2027-01-31';

  isDragging = false;
  isUploading = false;

  errorMessage: string | null = null;

  onFileSelected(event: Event): void {

    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      return;
    }

    this.selectFile(input.files[0]);
  }

  selectFile(file: File): void {

    this.errorMessage = null;

    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Please select an image file.';
      return;
    }

    if (file.size > 20 * 1024 * 1024) {
      this.errorMessage = 'The image must be smaller than 20 MB.';
      return;
    }

    this.selectedFile = file;

    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }

    this.previewUrl = URL.createObjectURL(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {

    event.preventDefault();
    this.isDragging = false;

    const file = event.dataTransfer?.files?.[0];

    if (file) {
      this.selectFile(file);
    }
  }

  removeFile(): void {

    this.selectedFile = null;

    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = null;
    }

    this.errorMessage = null;
  }

  canSubmit(): boolean {

    return !!this.selectedFile
      && !!this.semesterStart
      && !!this.semesterEnd
      && !this.isUploading;
  }

  submit(): void {

    if (!this.canSubmit() || !this.selectedFile) {
      return;
    }

    this.errorMessage = null;
    this.isUploading = true;

    this.api.extractTimetable(
      this.selectedFile,
      this.semesterStart,
      this.semesterEnd
    ).subscribe({

      next: response => {

        this.router.navigate([
          '/review',
          response.jobId
        ]);

      },

      error: error => {

        console.error(error);

        this.errorMessage =
          error?.error?.message
          ?? 'Unable to start timetable extraction.';

        this.isUploading = false;
      }

    });
  }
}

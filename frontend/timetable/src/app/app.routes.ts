import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'upload',
    pathMatch: 'full'
  },
  {
    path: 'upload',
    loadComponent: () =>
      import('./features/timetable/upload/upload.component')
        .then(m => m.UploadComponent)
  },
  {
    path: 'review/:jobId',
    loadComponent: () =>
      import('./features/timetable/review/review.component')
        .then(m => m.ReviewComponent)
  },
  {
    path: '**',
    redirectTo: 'upload'
  }
];

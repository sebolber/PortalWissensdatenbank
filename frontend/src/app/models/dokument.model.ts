export interface DokumentDto {
  id: string;
  tenantId: string;
  title: string;
  content: string;
  summary: string | null;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  categoryId: string | null;
  categoryName: string | null;
  tags: TagDto[];
  createdBy: string;
  updatedBy: string | null;
  createdAt: string;
  updatedAt: string | null;
  version: number;
  publicWithinTenant: boolean;
  viewCount: number;
  averageRating: number;
  ratingCount: number;
}

export interface TagDto {
  id: string;
  name: string;
}

export interface CategoryDto {
  id: string;
  name: string;
  description: string | null;
  parentId: string | null;
  orderIndex: number;
  createdAt: string;
}

export interface StatistikDto {
  totalDocuments: number;
  publishedDocuments: number;
  draftDocuments: number;
  archivedDocuments: number;
  totalCategories: number;
  totalTags: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface DokumentVersionDto {
  id: string;
  version: number;
  title: string;
  summary: string | null;
  changedBy: string;
  changedAt: string;
  changeNote: string | null;
}

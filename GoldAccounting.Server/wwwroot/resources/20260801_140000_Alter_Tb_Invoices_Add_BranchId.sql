/* ============================================================
 *  20260801_140000_Alter_Tb_Invoices_Add_BranchId.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 * ============================================================ */

IF COL_LENGTH(N'dbo.Invoices', N'BranchId') IS NULL
    ALTER TABLE dbo.Invoices ADD BranchId INT NOT NULL DEFAULT (0);

GO

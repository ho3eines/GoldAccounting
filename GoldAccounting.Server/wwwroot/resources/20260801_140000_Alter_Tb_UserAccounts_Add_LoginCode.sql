/* ============================================================
 *  20260801_140000_Alter_Tb_UserAccounts_Add_LoginCode.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 * ============================================================ */

IF COL_LENGTH(N'dbo.UserAccounts', N'LoginCode') IS NULL
    ALTER TABLE dbo.UserAccounts ADD LoginCode NVARCHAR(10) NOT NULL DEFAULT (N'');
IF COL_LENGTH(N'dbo.UserAccounts', N'LoginCodeExp') IS NULL
    ALTER TABLE dbo.UserAccounts ADD LoginCodeExp BIGINT NOT NULL DEFAULT (0);

GO

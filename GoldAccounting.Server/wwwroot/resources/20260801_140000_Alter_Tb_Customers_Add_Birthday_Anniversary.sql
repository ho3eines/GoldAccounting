/* ============================================================
 *  20260801_140000_Alter_Tb_Customers_Add_Birthday_Anniversary.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 * ============================================================ */

IF COL_LENGTH(N'dbo.Customers', N'BirthdayJ') IS NULL
    ALTER TABLE dbo.Customers ADD BirthdayJ NVARCHAR(20) NOT NULL DEFAULT (N'');
IF COL_LENGTH(N'dbo.Customers', N'AnniversaryJ') IS NULL
    ALTER TABLE dbo.Customers ADD AnniversaryJ NVARCHAR(20) NOT NULL DEFAULT (N'');
IF COL_LENGTH(N'dbo.Customers', N'SmsConsent') IS NULL
    ALTER TABLE dbo.Customers ADD SmsConsent BIT NOT NULL DEFAULT (1);

GO

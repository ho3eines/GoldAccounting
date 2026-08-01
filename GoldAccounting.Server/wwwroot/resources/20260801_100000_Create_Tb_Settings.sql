/* ============================================================
 *  20260801_100000_Create_Tb_Settings.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Settings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Settings
    (
        K NVARCHAR(200) NOT NULL CONSTRAINT PK_Settings PRIMARY KEY,
        V NVARCHAR(MAX) NULL
    );
END

GO

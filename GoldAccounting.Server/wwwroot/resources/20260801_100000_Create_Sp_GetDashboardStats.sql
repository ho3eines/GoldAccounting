/* ============================================================
 *  20260801_100000_Create_Sp_GetDashboardStats.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.usp_GetDashboardStats', N'P') IS NULL
    EXEC(N'CREATE PROCEDURE dbo.usp_GetDashboardStats AS SELECT 1 AS Ok;');
GO

ALTER PROCEDURE dbo.usp_GetDashboardStats
AS
BEGIN
    SET NOCOUNT ON;
    SELECT
        (SELECT COUNT(1) FROM dbo.Invoices)                    AS InvoiceCount,
        (SELECT COUNT(1) FROM dbo.Customers)                   AS CustomerCount,
        (SELECT COUNT(1) FROM dbo.AccountingVouchers)          AS VoucherCount,
        (SELECT COUNT(1) FROM dbo.SyncLogs)                    AS SyncCount,
        (SELECT COALESCE(SUM(Amount),0) FROM dbo.CashTransactions WHERE Kind = N'in')
      - (SELECT COALESCE(SUM(Amount),0) FROM dbo.CashTransactions WHERE Kind = N'out') AS CashBalance,
        (SELECT TOP(1) RateVal FROM dbo.Rates ORDER BY Ts DESC, Id DESC) AS CurrentRate;
END

GO

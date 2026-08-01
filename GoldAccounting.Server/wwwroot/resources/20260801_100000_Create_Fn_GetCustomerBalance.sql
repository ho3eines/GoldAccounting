/* ============================================================
 *  20260801_100000_Create_Fn_GetCustomerBalance.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.fn_GetCustomerBalance', N'FN') IS NOT NULL
    DROP FUNCTION dbo.fn_GetCustomerBalance;
GO

CREATE FUNCTION dbo.fn_GetCustomerBalance (@Cid INT)
RETURNS TABLE
AS
RETURN
(
    SELECT
        @Cid AS Cid,
        COALESCE(SUM(Cash),0)   AS CashBalance,
        COALESCE(SUM(Goldmw),0) AS GoldBalance
    FROM dbo.CustomerTxs
    WHERE Cid = @Cid
);

GO

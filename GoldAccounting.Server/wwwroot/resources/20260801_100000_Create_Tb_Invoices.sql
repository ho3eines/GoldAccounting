/* ============================================================
 *  20260801_100000_Create_Tb_Invoices.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  این اسکریپت یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 *  اسکریپت‌های ثبت‌شده دیگر اجرا نمی‌شوند.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Invoices', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Invoices
    (
        Id           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Invoices PRIMARY KEY,
        Ts           BIGINT            NOT NULL DEFAULT (0),
        DateJ        NVARCHAR(20)      NOT NULL DEFAULT (N''),
        Cid          INT               NOT NULL DEFAULT (0),
        Cname        NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Rate         BIGINT            NOT NULL DEFAULT (0),
        GoldVal      BIGINT            NOT NULL DEFAULT (0),
        Wage         BIGINT            NOT NULL DEFAULT (0),
        Stone        BIGINT            NOT NULL DEFAULT (0),
        Tax          BIGINT            NOT NULL DEFAULT (0),
        Total        BIGINT            NOT NULL DEFAULT (0),
        Pcash        BIGINT            NOT NULL DEFAULT (0),
        PgoldMw      BIGINT            NOT NULL DEFAULT (0),
        PgoldVal     BIGINT            NOT NULL DEFAULT (0),
        PgoldKarat   INT               NOT NULL DEFAULT (0),
        Debt         BIGINT            NOT NULL DEFAULT (0),
        Note         NVARCHAR(500)     NOT NULL DEFAULT (N''),
        TransferCode NVARCHAR(100)     NOT NULL DEFAULT (N'')
    );
    CREATE INDEX IX_Invoices_TransferCode ON dbo.Invoices(TransferCode);
    CREATE INDEX IX_Invoices_DateJ ON dbo.Invoices(DateJ);
END

GO

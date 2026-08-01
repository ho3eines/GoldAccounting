/* ============================================================
 *  20260801_140000_Create_Tb_Branches.sql
 *  طلایار — GoldAccounting (DB-First / SQL Server)
 *  یک‌بار اجرا شده و نتیجه در dbo.Resources ثبت می‌شود.
 * ============================================================ */

IF OBJECT_ID(N'dbo.Branches', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Branches
    (
        Id      INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Branches PRIMARY KEY,
        Code    NVARCHAR(50)      NOT NULL DEFAULT (N''),
        Name    NVARCHAR(200)     NOT NULL DEFAULT (N''),
        Address NVARCHAR(300)     NOT NULL DEFAULT (N''),
        Phone   NVARCHAR(50)      NOT NULL DEFAULT (N''),
        IsActive BIT              NOT NULL DEFAULT (1),
        Cts     BIGINT            NOT NULL DEFAULT (0)
    );
    CREATE UNIQUE INDEX UX_Branches_Code ON dbo.Branches(Code);
END

GO

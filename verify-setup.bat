#!/bin/bash
# Database & Project Verification Script
# Run this in CMD or PowerShell to verify setup

echo "========================================"
echo "🔍 PROJECT VERIFICATION REPORT"
echo "========================================"
echo ""

# 1. Check if directories exist
echo "1. CHECK STORAGE FOLDERS"
echo "-----"
if exist F:\anh\doan\atudw\users (
    echo "✅ F:\anh\doan\atudw\users exists"
) else (
    echo "❌ F:\anh\doan\atudw\users MISSING"
)

if exist F:\anh\doan\atudw\questions (
    echo "✅ F:\anh\doan\atudw\questions exists"
) else (
    echo "❌ F:\anh\doan\atudw\questions MISSING"
)

if exist F:\anh\doan\atudw\answers (
    echo "✅ F:\anh\doan\atudw\answers exists"
) else (
    echo "❌ F:\anh\doan\atudw\answers MISSING"
)
echo ""

# 2. Check Maven
echo "2. CHECK MAVEN"
echo "-----"
mvn -v
echo ""

# 3. Compile project
echo "3. COMPILE PROJECT"
echo "-----"
cd /d f:\web\springtool\atudw\abc\DOAN_LTW-main\DOAN_LTW-main
mvn clean compile -q
if %ERRORLEVEL% EQU 0 (
    echo "✅ Project compiles successfully"
) else (
    echo "❌ Compilation FAILED"
)
echo ""

# 4. Check Database Connection
echo "4. CHECK SQL SERVER DATABASE"
echo "-----"
sqlcmd -S localhost,1433 -U ltw_user -P 111111 -d LTW -Q "SELECT @@VERSION; SELECT COUNT(*) as TableCount FROM INFORMATION_SCHEMA.TABLES;"
if %ERRORLEVEL% EQU 0 (
    echo "✅ Database connection successful"
) else (
    echo "❌ Database connection FAILED"
    echo "   Check: SQL Server running? User/Password correct?"
)
echo ""

# 5. Check Flyway Migrations
echo "5. CHECK MIGRATIONS"
echo "-----"
sqlcmd -S localhost,1433 -U ltw_user -P 111111 -d LTW -Q "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY version;"
echo ""

# 6. Check Tables Created
echo "6. CHECK TABLES"
echo "-----"
sqlcmd -S localhost,1433 -U ltw_user -P 111111 -d LTW -Q "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='dbo' ORDER BY TABLE_NAME;"
echo ""

echo "========================================"
echo "✅ VERIFICATION COMPLETE"
echo "========================================"

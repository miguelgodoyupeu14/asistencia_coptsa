param(
    [string]$BaseUrl = 'https://admin.tail6d7b8e.ts.net/api/v1',
    [Parameter(Mandatory = $true)][string]$AdminPassword,
    [Parameter(Mandatory = $true)][string]$EmployeePassword,
    [Parameter(Mandatory = $true)][string]$SupervisorPassword
)

$ErrorActionPreference = 'Stop'
$results = [System.Collections.Generic.List[object]]::new()

function Add-Result([string]$Name, [bool]$Passed, [string]$Actual) {
    $results.Add([pscustomobject]@{ Test = $Name; Passed = $Passed; Actual = $Actual })
    if (-not $Passed) { throw "Falló '$Name': $Actual" }
}

function Login([string]$Username, [string]$Password) {
    (Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType 'application/json' -Body (@{ username = $Username; password = $Password } | ConvertTo-Json)).accessToken
}

function Headers([string]$Token) { @{ Authorization = "Bearer $Token" } }

function Call-Api([string]$Method, [string]$Path, [string]$Token, $Body = $null) {
    try {
        $args = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = Headers $Token }
        if ($null -ne $Body) { $args.ContentType = 'application/json'; $args.Body = $Body | ConvertTo-Json -Depth 8 }
        $value = Invoke-RestMethod @args
        [pscustomobject]@{ Status = 200; Detail = ''; Value = $value }
    } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        $detail = $_.ErrorDetails.Message
        if (-not $detail -and $_.Exception.Response) {
            try {
                $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $detail = $reader.ReadToEnd()
                $reader.Dispose()
            } catch {}
        }
        try { $detail = ($detail | ConvertFrom-Json).detail } catch {}
        [pscustomobject]@{ Status = $status; Detail = $detail; Value = $null }
    }
}

function Set-Policy($AdminToken, [long]$SiteId, [bool]$Geo, [bool]$Device, [bool]$Qr, [bool]$Wifi, [string]$Bssid = '') {
    $body = @{ geofenceRadiusM = 300; requireGeofence = $Geo; requireRegisteredDevice = $Device; requireDynamicQr = $Qr; allowedWifiBssid = $(if ($Bssid) { $Bssid } else { $null }); requireWifi = $Wifi }
    $result = Call-Api PATCH "/organization/sites/$SiteId/attendance-security" $AdminToken $body
    Add-Result 'Actualizar política de seguridad' ($result.Status -eq 200) "HTTP $($result.Status) $($result.Detail)"
}

function Punch($EmployeeToken, [string]$Type, [string]$Device, $Latitude, $Longitude, $QrCode, $Wifi) {
    Call-Api POST '/attendance/punch' $EmployeeToken @{ type = $Type; channel = 'AUTOMATED_TEST'; deviceIdentifier = $Device; latitude = $Latitude; longitude = $Longitude; qrCode = $QrCode; wifiBssid = $Wifi }
}

function Passed-Control($Result, [string[]]$SecurityErrors) {
    $Result.Status -eq 200 -or ($Result.Status -in 403,409 -and $Result.Detail -notin $SecurityErrors)
}

$admin = Login 'admin' $AdminPassword
$employee = Login 'empleado.iphone' $EmployeePassword
$supervisor = Login 'supervisor.pruebas' $SupervisorPassword
$runId = [Guid]::NewGuid().ToString('N')
$deviceIdentifier = "live-test-device-$runId"
$deviceDisplayName = "Prueba automatizada $runId"
Add-Result 'Login de los tres roles' ($admin -and $employee -and $supervisor) 'JWT emitidos'

$sites = (Call-Api GET '/organization/sites' $admin).Value
$site = $sites | Where-Object code -eq 'UPEU-LIMA'
if (-not $site) { throw 'No existe la sede UPEU-LIMA' }
$users = (Call-Api GET '/users' $admin).Value
$supervisorUser = $users | Where-Object username -eq 'supervisor.pruebas' | Select-Object -First 1
$existingScopes = (Call-Api GET '/supervisor-scopes' $admin).Value
if ($supervisorUser -and -not ($existingScopes | Where-Object { $_.user_id -eq $supervisorUser.id -and $_.site_id -eq $site.id })) {
    $scopeResult = Call-Api POST '/supervisor-scopes' $admin @{ userId = $supervisorUser.id; siteId = $site.id; areaId = $null }
    Add-Result 'Asignar alcance de sede al supervisor' ($scopeResult.Status -eq 200) "HTTP $($scopeResult.Status)"
}

try {
    $r = Call-Api GET '/users' $admin
    Add-Result 'ADMIN puede administrar usuarios' ($r.Status -eq 200) "HTTP $($r.Status)"
    $r = Call-Api GET '/users' $employee
    Add-Result 'EMPLOYEE no puede administrar usuarios' ($r.Status -eq 403) "HTTP $($r.Status)"
    $r = Call-Api GET '/requests?status=SUBMITTED' $supervisor
    Add-Result 'SUPERVISOR puede revisar solicitudes' ($r.Status -eq 200) "HTTP $($r.Status)"
    $r = Call-Api GET '/requests?status=SUBMITTED' $employee
    Add-Result 'EMPLOYEE no puede revisar solicitudes ajenas' ($r.Status -eq 403) "HTTP $($r.Status)"
    $today = Get-Date
    $from = $today.AddDays(-30).ToString('yyyy-MM-dd')
    $to = $today.ToString('yyyy-MM-dd')
    $r = Call-Api GET "/attendance/me?from=$from&to=$to" $employee
    Add-Result 'EMPLOYEE puede consultar su asistencia' ($r.Status -eq 200) "HTTP $($r.Status)"

    Set-Policy $admin $site.id $true $false $false $false
    $r = Punch $employee ENTRY $deviceIdentifier $null $null $null $null
    Add-Result 'Geocerca rechaza GPS ausente con motivo preciso' ($r.Status -eq 403 -and $r.Detail -eq 'GPS_REQUIRED') "HTTP $($r.Status) $($r.Detail)"
    $r = Punch $employee ENTRY $deviceIdentifier 0.0 0.0 $null $null
    Add-Result 'Geocerca rechaza coordenadas externas' ($r.Status -eq 403 -and $r.Detail -eq 'OUTSIDE_GEOFENCE') "HTTP $($r.Status) $($r.Detail)"
    $r = Punch $employee ENTRY $deviceIdentifier $site.latitude $site.longitude $null $null
    Add-Result 'Geocerca acepta coordenadas UPeU y continúa a reglas de negocio' (Passed-Control $r @('GPS_REQUIRED','GEOFENCE_NOT_CONFIGURED','OUTSIDE_GEOFENCE')) "HTTP $($r.Status) $($r.Detail)"

    Set-Policy $admin $site.id $true $true $false $false
    $r = Punch $employee ENTRY $deviceIdentifier $site.latitude $site.longitude $null $null
    Add-Result 'Dispositivo no aprobado es rechazado' ($r.Status -eq 403 -and $r.Detail -eq 'DEVICE_NOT_APPROVED') "HTTP $($r.Status) $($r.Detail)"
    $registered = Call-Api POST '/attendance-security/devices' $employee @{ identifier = $deviceIdentifier; displayName = $deviceDisplayName; platform = 'TEST' }
    Add-Result 'Empleado registra dispositivo' ($registered.Status -eq 200) "HTTP $($registered.Status)"
    $devices = (Call-Api GET '/attendance-security/devices' $admin).Value
    $device = $devices | Where-Object display_name -eq $deviceDisplayName | Select-Object -First 1
    $approved = Call-Api PATCH "/attendance-security/devices/$($device.id)" $admin @{ approved = $true }
    Add-Result 'Administrador aprueba dispositivo' ($approved.Status -eq 200) "HTTP $($approved.Status)"
    $r = Punch $employee ENTRY $deviceIdentifier $site.latitude $site.longitude $null $null
    Add-Result 'Dispositivo aprobado continúa a reglas de negocio' (Passed-Control $r @('DEVICE_REQUIRED','DEVICE_NOT_APPROVED')) "HTTP $($r.Status) $($r.Detail)"

    Set-Policy $admin $site.id $true $true $true $false
    $qr = (Call-Api POST '/attendance-security/qr' $admin @{ siteId = $site.id; validSeconds = 120 }).Value
    $r = Punch $employee ENTRY $deviceIdentifier $site.latitude $site.longitude 'codigo-invalido' $null
    Add-Result 'QR inválido es rechazado' ($r.Status -eq 403 -and $r.Detail -eq 'QR_INVALID') "HTTP $($r.Status) $($r.Detail)"
    $r = Punch $employee ENTRY $deviceIdentifier $site.latitude $site.longitude $qr.code $null
    Add-Result 'QR válido continúa a reglas de negocio' (Passed-Control $r @('QR_REQUIRED','QR_INVALID','QR_WRONG_SITE','QR_REPLACED','QR_EXPIRED')) "HTTP $($r.Status) $($r.Detail)"

    Set-Policy $admin $site.id $false $false $false $true 'AA:BB:CC:DD:EE:FF'
    $r = Punch $employee ENTRY $deviceIdentifier $null $null $null $null
    Add-Result 'Wi-Fi ausente es rechazado' ($r.Status -eq 403 -and $r.Detail -eq 'WIFI_REQUIRED') "HTTP $($r.Status) $($r.Detail)"
    $r = Punch $employee ENTRY $deviceIdentifier $null $null $null 'AA-BB-CC-DD-EE-FF'
    Add-Result 'BSSID autorizado continúa a reglas de negocio' (Passed-Control $r @('WIFI_REQUIRED','INVALID_WIFI')) "HTTP $($r.Status) $($r.Detail)"

    Set-Policy $admin $site.id $true $false $false $false
    $r = Punch $employee EXIT $deviceIdentifier $site.latitude $site.longitude $null $null
    Add-Result 'Salida respeta la secuencia de jornada' ($r.Status -in 200,403,409 -and $r.Status -ne 500) "HTTP $($r.Status) $($r.Detail)"

    $affectedDate = $today.ToString('yyyy-MM-dd')
    $permissionFrom = $today.ToUniversalTime().ToString('o')
    $permissionTo = $today.AddHours(1).ToUniversalTime().ToString('o')
    $request = (Call-Api POST '/requests' $employee @{ type = 'PERMISSION'; affectedDate = $affectedDate; reason = '[PRUEBA AUTOMATIZADA] Flujo empleado-supervisor'; requestedFrom = $permissionFrom; requestedTo = $permissionTo; proposedPunchType = $null; proposedAt = $null; requestedChange = 'Validación automática'; evidenceReference = $null }).Value
    Add-Result 'Empleado crea solicitud' ($null -ne $request.id -and $request.status -eq 'SUBMITTED') "Solicitud $($request.id), estado $($request.status)"
    $review = (Call-Api PATCH "/requests/$($request.id)/review" $supervisor @{ decision = 'REJECTED'; comment = 'Rechazada por prueba automatizada controlada' }).Value
    Add-Result 'Supervisor resuelve solicitud dentro de su alcance' ($review.status -eq 'REJECTED') "Solicitud $($review.id), estado $($review.status)"
} finally {
    Set-Policy $admin $site.id $true $false $false $false
}

$results | Format-Table -AutoSize
"TOTAL=$($results.Count) PASSED=$(($results | Where-Object Passed).Count) FAILED=$(($results | Where-Object { -not $_.Passed }).Count)"

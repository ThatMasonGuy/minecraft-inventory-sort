param(
    [switch]$DryRun,
    [switch]$ReplaceGallery
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

$repoRoot = Split-Path -Parent $PSScriptRoot
$apiBase = 'https://api.modrinth.com/v2'
$userAgent = 'ThatMasonGuy/minecraft-inventory-sort-gallery-sync'

$projects = @(
    @{
        Section = 'Inventory Sort'
        GalleryDir = 'InvSort'
        ProjectIdProperty = 'modrinth_inventorysort_project_id'
    },
    @{
        Section = 'Inventory Search'
        GalleryDir = 'InvSearch'
        ProjectIdProperty = 'modrinth_inventorysearch_project_id'
    },
    @{
        Section = 'Inventory Catalogue'
        GalleryDir = 'InvCatalogue'
        ProjectIdProperty = 'modrinth_inventorycatalogue_project_id'
    }
)

function ConvertTo-QueryValue([string]$Value) {
    return [System.Uri]::EscapeDataString($Value)
}

function Get-ModrinthToken {
    if (-not [string]::IsNullOrWhiteSpace($env:MODRINTH_TOKEN)) {
        return $env:MODRINTH_TOKEN.Trim()
    }

    $envPath = Join-Path $repoRoot '.env'
    if (-not (Test-Path -LiteralPath $envPath)) {
        return $null
    }

    foreach ($line in Get-Content -LiteralPath $envPath) {
        if ($line -match '^\s*MODRINTH_TOKEN\s*=\s*(.+?)\s*$') {
            return $Matches[1].Trim().Trim('"').Trim("'")
        }
    }

    return $null
}

function Read-GradleProperties {
    $path = Join-Path $repoRoot 'gradle.properties'
    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $path) {
        if ($line -match '^\s*([^#][^=]+?)\s*=\s*(.*?)\s*$') {
            $properties[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    return $properties
}

function Read-PageSections {
    $path = Join-Path $repoRoot 'gradle/modrinth-project-pages.md'
    $markdown = Get-Content -LiteralPath $path -Raw
    $sections = @{}

    for ($i = 0; $i -lt $projects.Count; $i++) {
        $project = $projects[$i]
        $nextHeading = if ($i -lt ($projects.Count - 1)) { [regex]::Escape($projects[$i + 1].Section) } else { $null }
        $pattern = if ($nextHeading) {
            '(?s)^## ' + [regex]::Escape($project.Section) + '\r?\n(?<section>.*?)(?=^## ' + $nextHeading + '\r?\n)'
        } else {
            '(?s)^## ' + [regex]::Escape($project.Section) + '\r?\n(?<section>.*)\z'
        }

        $match = [regex]::Match($markdown, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
        if (-not $match.Success) {
            throw "Could not find project page section '$($project.Section)'."
        }

        $section = $match.Groups['section'].Value
        $summaryMatch = [regex]::Match($section, '(?s)- Summary:\s*\r?\n\r?\n(?<summary>.*?)(?=\r?\n\r?\n### Description Markdown)')
        if (-not $summaryMatch.Success) {
            throw "Could not find summary in '$($project.Section)'."
        }

        $bodyMatch = [regex]::Match($section, '(?s)```markdown\r?\n(?<body>.*?)\r?\n```')
        if (-not $bodyMatch.Success) {
            throw "Could not find description markdown fence in '$($project.Section)'."
        }

        $sections[$project.GalleryDir] = @{
            Summary = $summaryMatch.Groups['summary'].Value.Trim()
            Body = $bodyMatch.Groups['body'].Value.Trim()
        }
    }

    return $sections
}

function Read-GalleryMetadata {
    $path = Join-Path $repoRoot 'gallery/metadata.json'
    return Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
}

function Get-OrderFromFileName([string]$FileName) {
    if ($FileName -notmatch '^(\d+)_') {
        throw "Gallery image '$FileName' must start with a numeric order prefix."
    }
    return [int]$Matches[1]
}

function Get-LocalGalleryPlan($Project, $Metadata) {
    $galleryDir = Join-Path $repoRoot ('gallery/' + $Project.GalleryDir)
    $bannerDir = Join-Path $galleryDir 'banner'
    $descriptionDir = Join-Path $galleryDir 'description_images'

    $rootImages = @(Get-ChildItem -LiteralPath $galleryDir -File -Filter '*.png' |
        Sort-Object @{ Expression = { Get-OrderFromFileName $_.Name } }, Name)
    $bannerImages = @(Get-ChildItem -LiteralPath $bannerDir -File -Filter '*.png')
    $descriptionImages = @(Get-ChildItem -LiteralPath $descriptionDir -File -Filter '*.png' |
        Sort-Object @{ Expression = { Get-OrderFromFileName $_.Name } }, Name)

    if ($bannerImages.Count -ne 1) {
        throw "$($Project.GalleryDir) must have exactly one banner selector image."
    }

    $metadataProject = $Metadata.($Project.GalleryDir)
    if ($null -eq $metadataProject) {
        throw "Missing metadata for $($Project.GalleryDir)."
    }

    $metadataByFile = @{}
    foreach ($image in @($metadataProject.images)) {
        $metadataByFile[$image.file] = $image
    }

    $rootNames = @($rootImages | ForEach-Object Name)
    $metadataNames = @(@($metadataProject.images) | ForEach-Object file)
    foreach ($name in $rootNames) {
        if (-not $metadataByFile.ContainsKey($name)) {
            throw "Missing metadata entry for $($Project.GalleryDir)/$name."
        }
    }
    foreach ($name in $metadataNames) {
        if ($rootNames -notcontains $name) {
            throw "Metadata references missing gallery image $($Project.GalleryDir)/$name."
        }
    }

    $bannerName = $bannerImages[0].Name
    if ($rootNames -notcontains $bannerName) {
        throw "$($Project.GalleryDir) banner selector '$bannerName' is not one of the root gallery images."
    }

    $descriptionNames = @()
    foreach ($descriptionImage in $descriptionImages) {
        if ($rootNames -notcontains $descriptionImage.Name) {
            throw "$($Project.GalleryDir) description selector '$($descriptionImage.Name)' is not one of the root gallery images."
        }
        $descriptionNames += $descriptionImage.Name
    }

    $items = @()
    foreach ($rootImage in $rootImages) {
        $imageMetadata = $metadataByFile[$rootImage.Name]
        $items += [PSCustomObject]@{
            File = $rootImage.Name
            Path = $rootImage.FullName
            Order = Get-OrderFromFileName $rootImage.Name
            Title = [string]$imageMetadata.title
            Description = [string]$imageMetadata.description
            Featured = ($rootImage.Name -eq $bannerName)
            InDescription = ($descriptionNames -contains $rootImage.Name)
        }
    }

    return $items
}

function New-ModrinthHeaders([string]$Token, [switch]$Json) {
    $headers = @{
        'User-Agent' = $userAgent
        'Accept' = 'application/json'
    }

    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers['Authorization'] = $Token
    }

    if ($Json) {
        $headers['Content-Type'] = 'application/json'
    }

    return $headers
}

function Invoke-ModrinthWrite([string]$Method, [string]$Uri, [string]$Token, [string]$Body = $null) {
    $client = [System.Net.Http.HttpClient]::new()
    $request = $null
    $content = $null
    try {
        $client.DefaultRequestHeaders.UserAgent.ParseAdd($userAgent)
        $client.DefaultRequestHeaders.Accept.ParseAdd('application/json')
        $client.DefaultRequestHeaders.TryAddWithoutValidation('Authorization', $Token) | Out-Null

        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), $Uri)
        if ($null -ne $Body) {
            $content = [System.Net.Http.StringContent]::new($Body, [System.Text.Encoding]::UTF8, 'application/json')
            $request.Content = $content
        }

        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            throw "Modrinth $Method request failed with HTTP $([int]$response.StatusCode): $responseBody"
        }
    } finally {
        if ($null -ne $content) {
            $content.Dispose()
        }
        if ($null -ne $request) {
            $request.Dispose()
        }
        $client.Dispose()
    }
}

function Get-ModrinthProject([string]$ProjectId, [string]$Token) {
    $headers = New-ModrinthHeaders -Token $Token
    return Invoke-RestMethod -Method Get -Uri "$apiBase/project/$ProjectId" -Headers $headers
}

function Remove-GalleryImage([string]$ProjectId, [string]$Token, [string]$Url) {
    $encodedUrl = ConvertTo-QueryValue $Url
    Invoke-ModrinthWrite -Method 'DELETE' -Uri "$apiBase/project/$ProjectId/gallery?url=$encodedUrl" -Token $Token
}

function Send-GalleryImage([string]$ProjectId, [string]$Token, $Item) {
    $query = @(
        'ext=png'
        'featured=' + ([string]$Item.Featured).ToLowerInvariant()
        'title=' + (ConvertTo-QueryValue $Item.Title)
        'description=' + (ConvertTo-QueryValue $Item.Description)
        'ordering=' + $Item.Order
    ) -join '&'

    $client = [System.Net.Http.HttpClient]::new()
    $content = $null
    try {
        $client.DefaultRequestHeaders.UserAgent.ParseAdd($userAgent)
        $client.DefaultRequestHeaders.Accept.ParseAdd('application/json')
        $client.DefaultRequestHeaders.TryAddWithoutValidation('Authorization', $Token) | Out-Null

        $bytes = [System.IO.File]::ReadAllBytes($Item.Path)
        $content = [System.Net.Http.ByteArrayContent]::new($bytes)
        $content.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('image/png')

        $response = $client.PostAsync("$apiBase/project/$ProjectId/gallery?$query", $content).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            throw "Gallery upload failed for '$($Item.File)' with HTTP $([int]$response.StatusCode): $body"
        }
    } finally {
        if ($null -ne $content) {
            $content.Dispose()
        }
        $client.Dispose()
    }
}

function Update-GalleryImageMetadata([string]$ProjectId, [string]$Token, [string]$Url, $Item) {
    $query = @(
        'url=' + (ConvertTo-QueryValue $Url)
        'featured=' + ([string]$Item.Featured).ToLowerInvariant()
        'title=' + (ConvertTo-QueryValue $Item.Title)
        'description=' + (ConvertTo-QueryValue $Item.Description)
        'ordering=' + $Item.Order
    ) -join '&'

    Invoke-ModrinthWrite -Method 'PATCH' -Uri "$apiBase/project/$ProjectId/gallery?$query" -Token $Token
}

function Resolve-BodyGalleryUrls([string]$GalleryDir, [string]$Body, $Items, $LiveProject) {
    $galleryByTitle = @{}
    foreach ($galleryImage in $LiveProject.gallery) {
        if (-not [string]::IsNullOrWhiteSpace($galleryImage.title)) {
            $galleryByTitle[$galleryImage.title] = $galleryImage
        }
    }

    $rendered = $Body
    foreach ($item in $Items) {
        $liveImage = $galleryByTitle[$item.Title]
        if ($null -eq $liveImage) {
            throw "Could not find uploaded gallery image '$($item.Title)' on $GalleryDir."
        }

        if ($item.InDescription) {
            $placeholder = "modrinth-gallery://$GalleryDir/$($item.File)"
            if (-not $rendered.Contains($placeholder)) {
                throw "Description for $GalleryDir does not contain expected placeholder '$placeholder'."
            }
            $rendered = $rendered.Replace($placeholder, [string]$liveImage.url)
        }
    }

    if ($rendered -match 'modrinth-gallery://') {
        throw "Unresolved gallery placeholder remains in $GalleryDir description."
    }

    return $rendered
}

function Update-ModrinthProjectPage([string]$ProjectId, [string]$Token, [string]$Summary, [string]$Body) {
    $payload = @{
        description = $Summary
        body = $Body
    } | ConvertTo-Json -Depth 4

    Invoke-ModrinthWrite -Method 'PATCH' -Uri "$apiBase/project/$ProjectId" -Token $Token -Body $payload
}

$gradleProperties = Read-GradleProperties
$pageSections = Read-PageSections
$metadata = Read-GalleryMetadata
$token = Get-ModrinthToken

if (-not $DryRun -and [string]::IsNullOrWhiteSpace($token)) {
    throw 'MODRINTH_TOKEN is required for live sync. Set it in the environment or .env.'
}

foreach ($project in $projects) {
    $projectId = $gradleProperties[$project.ProjectIdProperty]
    if ([string]::IsNullOrWhiteSpace($projectId)) {
        throw "Missing Gradle property $($project.ProjectIdProperty)."
    }

    $items = Get-LocalGalleryPlan -Project $project -Metadata $metadata
    $page = $pageSections[$project.GalleryDir]

    Write-Host "Project $($project.GalleryDir): $($items.Count) gallery image(s), $(@($items | Where-Object InDescription).Count) description image(s)."

    foreach ($item in $items) {
        $featured = if ($item.Featured) { 'featured' } else { 'gallery' }
        Write-Host "  [$($item.Order)] $($item.Title) ($featured)"
    }

    foreach ($item in @($items | Where-Object InDescription)) {
        $placeholder = "modrinth-gallery://$($project.GalleryDir)/$($item.File)"
        if (-not ([string]$page.Body).Contains($placeholder)) {
            throw "Description for $($project.GalleryDir) does not reference selected image '$placeholder'."
        }
    }

    if ($DryRun) {
        continue
    }

    $liveProject = Get-ModrinthProject -ProjectId $projectId -Token $token
    if ($ReplaceGallery) {
        foreach ($galleryImage in @($liveProject.gallery)) {
            Remove-GalleryImage -ProjectId $projectId -Token $token -Url $galleryImage.url
        }
        $liveProject = Get-ModrinthProject -ProjectId $projectId -Token $token
    }

    $liveByTitle = @{}
    foreach ($galleryImage in @($liveProject.gallery)) {
        if (-not [string]::IsNullOrWhiteSpace($galleryImage.title)) {
            $liveByTitle[$galleryImage.title] = $galleryImage
        }
    }

    foreach ($item in $items) {
        $existing = $liveByTitle[$item.Title]
        if ($null -eq $existing) {
            Send-GalleryImage -ProjectId $projectId -Token $token -Item $item
        } else {
            Update-GalleryImageMetadata -ProjectId $projectId -Token $token -Url $existing.url -Item $item
        }
    }

    $liveProject = Get-ModrinthProject -ProjectId $projectId -Token $token
    $renderedBody = Resolve-BodyGalleryUrls -GalleryDir $project.GalleryDir -Body $page.Body -Items $items -LiveProject $liveProject
    Update-ModrinthProjectPage -ProjectId $projectId -Token $token -Summary $page.Summary -Body $renderedBody

    $readback = Get-ModrinthProject -ProjectId $projectId -Token $token
    if ($readback.description -ne $page.Summary) {
        throw "Readback summary mismatch for $($project.GalleryDir)."
    }
    if ($readback.body -ne $renderedBody) {
        throw "Readback body mismatch for $($project.GalleryDir)."
    }
    if (@($readback.gallery).Count -lt $items.Count) {
        throw "Readback gallery count for $($project.GalleryDir) is lower than expected."
    }

    Write-Host "  Synced $($readback.title) ($($readback.slug)): $(@($readback.gallery).Count) gallery image(s)."
}

if ($DryRun) {
    Write-Host 'Dry run complete. No Modrinth API writes were made.'
} else {
    Write-Host 'Modrinth project page sync complete.'
}

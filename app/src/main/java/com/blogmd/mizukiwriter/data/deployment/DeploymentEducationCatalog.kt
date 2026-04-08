package com.blogmd.mizukiwriter.data.deployment

import com.blogmd.mizukiwriter.data.settings.DeploymentPlatform

data class DeploymentFieldDoc(
    val label: String,
    val help: String,
    val example: String,
    val referenceLabel: String,
    val referenceUrl: String,
)

data class DeploymentGuideSection(
    val title: String,
    val body: String,
)

data class DeploymentReferenceLink(
    val label: String,
    val url: String,
)

data class DeploymentGuideArticle(
    val title: String,
    val intro: String,
    val sections: List<DeploymentGuideSection>,
    val referenceLinks: List<DeploymentReferenceLink>,
)

object DeploymentEducationCatalog {
    fun fieldDocsFor(platform: DeploymentPlatform): List<DeploymentFieldDoc> = when (platform) {
        DeploymentPlatform.Vercel -> listOf(
            DeploymentFieldDoc(
                label = "部署平台 Token",
                help = "在 Vercel 账号设置里创建 Personal Token，应用用它来查询项目、创建项目和绑定域名。",
                example = "例如：vercel_xxxxxxxxx",
                referenceLabel = "Vercel Token 教程",
                referenceUrl = "https://vercel.com/account/tokens",
            ),
            DeploymentFieldDoc(
                label = "部署项目名",
                help = "这是 Vercel 里的项目名称。首次创建项目时会直接使用这个名称。",
                example = "例如：mizuki-blog",
                referenceLabel = "Vercel 项目文档",
                referenceUrl = "https://vercel.com/docs/projects/overview",
            ),
            DeploymentFieldDoc(
                label = "平台 Team ID",
                help = "如果项目建在团队空间，需要填 Team ID；个人空间可以留空。",
                example = "例如：team_abc123",
                referenceLabel = "Vercel Team 参数说明",
                referenceUrl = "https://vercel.com/docs/rest-api/reference#project-members-and-roles",
            ),
            DeploymentFieldDoc(
                label = "构建命令",
                help = "Mizuki 基于 Astro，常见命令是 pnpm build。除非仓库做过改造，否则不要改。",
                example = "pnpm build",
                referenceLabel = "Mizuki 部署文档",
                referenceUrl = "https://docs.mizuki.mysqil.com/en/guide/deployment/",
            ),
            DeploymentFieldDoc(
                label = "产物目录",
                help = "Astro 构建后的静态文件目录，默认是 dist。",
                example = "dist",
                referenceLabel = "Vercel Astro 文档",
                referenceUrl = "https://vercel.com/docs/frameworks/frontend/astro",
            ),
        )

        DeploymentPlatform.CloudflarePages -> listOf(
            DeploymentFieldDoc(
                label = "部署平台 Token",
                help = "Cloudflare API Token 需要有 Pages 项目与域名相关权限，否则无法创建项目。",
                example = "例如：cf_xxxxx",
                referenceLabel = "Cloudflare API Token 教程",
                referenceUrl = "https://developers.cloudflare.com/fundamentals/api/get-started/create-token/",
            ),
            DeploymentFieldDoc(
                label = "平台 Account ID",
                help = "Pages 项目归属于某个 Cloudflare 账号，Account ID 必填，可在账户概览页找到。",
                example = "例如：a1b2c3d4e5",
                referenceLabel = "Cloudflare Account ID 说明",
                referenceUrl = "https://developers.cloudflare.com/fundamentals/setup/find-account-and-zone-ids/",
            ),
            DeploymentFieldDoc(
                label = "部署项目名",
                help = "Cloudflare Pages 的项目名称，通常会同时影响默认 pages.dev 域名。",
                example = "例如：mizuki-pages",
                referenceLabel = "Pages 项目文档",
                referenceUrl = "https://developers.cloudflare.com/pages/",
            ),
            DeploymentFieldDoc(
                label = "构建命令",
                help = "默认填 pnpm build。除非你已经确认仓库的脚本不同，否则保持默认。",
                example = "pnpm build",
                referenceLabel = "Mizuki 部署文档",
                referenceUrl = "https://docs.mizuki.mysqil.com/en/guide/deployment/",
            ),
            DeploymentFieldDoc(
                label = "产物目录",
                help = "Pages 上传的是静态产物目录，Mizuki/Astro 默认是 dist。",
                example = "dist",
                referenceLabel = "Cloudflare Pages 构建配置",
                referenceUrl = "https://developers.cloudflare.com/pages/configuration/build-configuration/",
            ),
        )

        DeploymentPlatform.EdgeOnePages -> listOf(
            DeploymentFieldDoc(
                label = "部署项目名",
                help = "EdgeOne Pages 项目名。当前应用优先为仓库生成 GitHub Actions 工作流来调用 edgeone CLI。",
                example = "例如：mizuki-edgeone",
                referenceLabel = "EdgeOne Pages 文档",
                referenceUrl = "https://pages.edgeone.ai/document",
            ),
            DeploymentFieldDoc(
                label = "部署平台 Token",
                help = "EdgeOne Token 更适合作为 GitHub Secret，而不是直接长期保存在应用里。",
                example = "建议命名为 EDGEONE_API_TOKEN",
                referenceLabel = "EdgeOne CLI 文档",
                referenceUrl = "https://pages.edgeone.ai/document/edgeone-cli",
            ),
            DeploymentFieldDoc(
                label = "部署工作流名",
                help = "用于在部署中心聚焦某个 GitHub Actions 工作流，方便查看 EdgeOne 的部署记录。",
                example = "例如：Deploy EdgeOne Pages",
                referenceLabel = "GitHub Actions 集成",
                referenceUrl = "https://pages.edgeone.ai/document/use-github-actions",
            ),
            DeploymentFieldDoc(
                label = "构建命令",
                help = "EdgeOne GitHub Actions 工作流也会使用你的构建命令，Mizuki 默认仍是 pnpm build。",
                example = "pnpm build",
                referenceLabel = "Mizuki 部署文档",
                referenceUrl = "https://docs.mizuki.mysqil.com/en/guide/deployment/",
            ),
        )
    }

    fun guideFor(platform: DeploymentPlatform): DeploymentGuideArticle = when (platform) {
        DeploymentPlatform.Vercel -> DeploymentGuideArticle(
            title = "Vercel 部署教程",
            intro = "适合希望快速上线并拿到 vercel.app 域名的博客用户。",
            sections = listOf(
                DeploymentGuideSection("第 1 步：准备 GitHub 仓库", "先在应用的设置页填好 GitHub 仓库、分支和 PAT，确保文章可以正常发布到仓库。"),
                DeploymentGuideSection("第 2 步：准备 Vercel Token", "在 Vercel 账号页面创建 Personal Token，回到应用填入部署平台 Token。"),
                DeploymentGuideSection("第 3 步：填写项目参数", "项目名、可选 Team ID、构建命令和产物目录确认后，从部署中心执行“创建或校验项目”。"),
                DeploymentGuideSection("第 4 步：确认上线结果", "创建成功后检查生产域名和最近部署记录，确认博客已在 Vercel 正常可访问。"),
                DeploymentGuideSection("第 5 步：绑定自定义域名", "在部署设置中填入域名后回到部署中心提交绑定，再按 Vercel 提示配置 DNS。"),
            ),
            referenceLinks = listOf(
                DeploymentReferenceLink("Vercel Token", "https://vercel.com/account/tokens"),
                DeploymentReferenceLink("Vercel 项目文档", "https://vercel.com/docs/projects/overview"),
                DeploymentReferenceLink("Mizuki 部署文档", "https://docs.mizuki.mysqil.com/en/guide/deployment/"),
            ),
        )

        DeploymentPlatform.CloudflarePages -> DeploymentGuideArticle(
            title = "Cloudflare Pages 部署教程",
            intro = "适合希望使用 pages.dev 与 Cloudflare 域名体系的博客用户。",
            sections = listOf(
                DeploymentGuideSection("第 1 步：准备 GitHub 仓库", "先确保 GitHub 仓库已在应用中配置完成，内容推送没有问题。"),
                DeploymentGuideSection("第 2 步：准备 Cloudflare API Token", "创建具备 Pages 权限的 Token，并确认你知道正确的 Account ID。"),
                DeploymentGuideSection("第 3 步：填写 Pages 参数", "在部署设置页填写 Token、Account ID、项目名、构建命令和产物目录。"),
                DeploymentGuideSection("第 4 步：创建或校验项目", "回到部署中心，点击“创建或校验项目”，确认 pages.dev 域名已返回。"),
                DeploymentGuideSection("第 5 步：绑定域名", "填入自定义域名后，从部署中心提交绑定，再按 Cloudflare 提示配置 DNS。"),
            ),
            referenceLinks = listOf(
                DeploymentReferenceLink("Cloudflare Token 教程", "https://developers.cloudflare.com/fundamentals/api/get-started/create-token/"),
                DeploymentReferenceLink("Account ID 说明", "https://developers.cloudflare.com/fundamentals/setup/find-account-and-zone-ids/"),
                DeploymentReferenceLink("Pages 文档", "https://developers.cloudflare.com/pages/"),
            ),
        )

        DeploymentPlatform.EdgeOnePages -> DeploymentGuideArticle(
            title = "EdgeOne Pages 部署教程",
            intro = "当前推荐通过 GitHub Actions 调用 edgeone CLI，这样比直接在手机内执行 CLI 更稳定。",
            sections = listOf(
                DeploymentGuideSection("第 1 步：准备 GitHub 仓库", "先在一级设置页完成 GitHub 仓库与 PAT 配置，确保博客内容能正常推送。"),
                DeploymentGuideSection("第 2 步：准备 EdgeOne Token", "在 EdgeOne 控制台获取 Token，建议把它存到 GitHub 仓库 Secret 中，而不是长期放在应用里。"),
                DeploymentGuideSection("第 3 步：填写部署参数", "在部署设置页填写项目名和工作流相关字段，执行方式优先选择 GitHub Actions。"),
                DeploymentGuideSection("第 4 步：生成工作流", "从部署中心执行“创建或校验项目”，应用会向仓库写入 EdgeOne 的 GitHub Actions 工作流。"),
                DeploymentGuideSection("第 5 步：完成 GitHub Secret 配置", "按教程将 EDGEONE_API_TOKEN 加入仓库 Secrets，然后推送一次内容或手动触发工作流。"),
            ),
            referenceLinks = listOf(
                DeploymentReferenceLink("EdgeOne CLI 文档", "https://pages.edgeone.ai/document/edgeone-cli"),
                DeploymentReferenceLink("EdgeOne GitHub Actions 文档", "https://pages.edgeone.ai/document/use-github-actions"),
                DeploymentReferenceLink("Mizuki 部署文档", "https://docs.mizuki.mysqil.com/en/guide/deployment/"),
            ),
        )
    }
}

listOf("en").map { lang ->
  Extension(
    name = "MyLoveNovel",
    versionCode = 3,
    libVersion = "1.3",
    lang = lang,
    description = "",
    nsfw = false,
  )
}.also(::register)

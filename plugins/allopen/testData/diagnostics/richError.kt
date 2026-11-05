// LANGUAGE: +RichErrors
annotation class AllOpen

@AllOpen error class <!NON_FINAL_RICH_ERROR_CLASS!>Foo<!>

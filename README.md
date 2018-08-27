# CheckDuplicateRes
A gradle plugin checking the duplicate resources of all local modules.

Add `apply plugin: 'CheckDuplicatePlugin'` to your app module.

### output is just like:

    CheckDuplicatePlugin: ==================================================>>>
    CheckDuplicatePlugin: ============ below are duplicate resources =======>>>
    CheckDuplicatePlugin: ==================================================>>>
    R$color.color_xiaobo, 2:
    [lib-test2, lib-test1]

    R$dimen.dimen_xiaobo, 2:
    [lib-test2, lib-test1]

    R$drawable.xiaobo_drawable, 2:
    [lib-test2, lib-test1]

    R$layout.layout_xiaobo, 2:
    [lib-test2, lib-test1]

    R$string.string_xiaobo, 2:
    [lib-test2, lib-test1]

    total size: 5
    CheckDuplicatePlugin: <<<==================================================
    CheckDuplicatePlugin: <<<========= above are duplicate resources =========
    CheckDuplicatePlugin: <<<==================================================


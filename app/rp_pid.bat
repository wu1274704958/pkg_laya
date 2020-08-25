echo %1 %2
replace_wxid src\main\AndroidManifest.xml %2 %1
replace_wxid build.gradle %2 %1
replace_wxid src\main\java\com\huolong\hf\utils\NewPkgMgr.java %2 %1
replace_wxid src\main\java\com\huolong\hf\ExternCall.java %2 %1
replace_wxid src\main\java\com\huolong\hf\MainActivity.java %2 %1
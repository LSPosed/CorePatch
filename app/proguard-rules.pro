#-repackageclasses com.coderstory.flyme.utils
-mergeinterfacesaggressively
-dontusemixedcaseclassnames
#指定代码的压缩级别
-optimizationpasses 7
-overloadaggressively
-useuniqueclassmembernames
#包明不混合大小写
#-dontusemixedcaseclassnames
#不去忽略非公共的库类
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

#预校验
-dontpreverify
#混淆时是否记录日志
#-verbose
#混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!code/simplication/cast,!field/*,!class/mergin/*
#避免混淆Annotation、内部类、泛型、匿名类
#-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

#保护注解
#-keepattributes Annotation
#保持哪些类不被混淆
-keep class toolkit.coderstory.CorePatch


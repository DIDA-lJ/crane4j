(window.webpackJsonp=window.webpackJsonp||[]).push([[32],{308:function(a,t,v){"use strict";v.r(t);var _=v(14),e=Object(_.a)({},(function(){var a=this,t=a._self._c;return t("ContentSlotsDistributor",{attrs:{"slot-key":a.$parent.slotKey}},[t("h2",{attrs:{id:"一、什么是配置解析器"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#一、什么是配置解析器"}},[a._v("#")]),a._v(" 一、什么是配置解析器")]),a._v(" "),t("p",[a._v("// TODO")]),a._v(" "),t("h2",{attrs:{id:"二、运行流程"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#二、运行流程"}},[a._v("#")]),a._v(" 二、运行流程")]),a._v(" "),t("ul",[t("li",[a._v("元素分拣，"),t("code",[a._v("Class")]),a._v(" / "),t("code",[a._v("Field")]),a._v(" 其他；")]),a._v(" "),t("li",[a._v("层级结构扫描；")]),a._v(" "),t("li",[a._v("操作注解处理器链的执行；")]),a._v(" "),t("li",[a._v("将注解解析为操作配置；")]),a._v(" "),t("li",[a._v("2 + 1 级缓存，与最终配置的组装；")]),a._v(" "),t("li",[a._v("未完善的 "),t("code",[a._v("active")]),a._v(" 问题；")])]),a._v(" "),t("h2",{attrs:{id:"三、不同版本的实现"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#三、不同版本的实现"}},[a._v("#")]),a._v(" 三、不同版本的实现")]),a._v(" "),t("h3",{attrs:{id:"_1、crane-时期"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#_1、crane-时期"}},[a._v("#")]),a._v(" 1、crane 时期")]),a._v(" "),t("p",[a._v("类级注解解析器 "),t("code",[a._v("ClassAnnotationConfigurationParser")]),a._v(" 与属性注解解析器 "),t("code",[a._v("FieldAnnotationConfigurationParser")]),a._v("。")]),a._v(" "),t("h3",{attrs:{id:"_2、v1-0-1-2"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#_2、v1-0-1-2"}},[a._v("#")]),a._v(" 2、v1.0  - 1.2")]),a._v(" "),t("p",[a._v("通用解析器 "),t("code",[a._v("AnnotationAwareBeanOperationParser")]),a._v(" 的出现，将 "),t("code",[a._v("Class")]),a._v(" 与 "),t("code",[a._v("Field")]),a._v(" 合二为一。")]),a._v(" "),t("h3",{attrs:{id:"_3、v1-3"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#_3、v1-3"}},[a._v("#")]),a._v(" 3、v1.3")]),a._v(" "),t("p",[a._v("v1.3 只有预览版本而无正式版，严格真正的 "),t("code",[a._v("2.0.0-ALPHA")]),a._v(" 其实是它。")]),a._v(" "),t("p",[a._v("层级化扫描注解解析器 "),t("code",[a._v("TypeHierarchyBeanOperationParser")]),a._v(" 与操作注解解析器 "),t("code",[a._v("OperationAnnotationResolver")]),a._v("。\n流水线模式，围绕同一个上下文的装配操作。")]),a._v(" "),t("h3",{attrs:{id:"_4、v2-0"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#_4、v2-0"}},[a._v("#")]),a._v(" 4、v2.0")]),a._v(" "),t("p",[a._v("操作注解处理器 "),t("code",[a._v("OperationAnnotationHandler")]),a._v("，职责更加明确，使用更灵活。")]),a._v(" "),t("p",[a._v("支持处理一切基于 "),t("code",[a._v("AnnotatedElement")]),a._v("，为操作者接口的实现提供了基础。")]),a._v(" "),t("p",[a._v("参考 spring 扫描/构建 "),t("code",[a._v("BeanDefinition")]),a._v(" 的模式，每种注解、每个层级结构的解析结果进行独立解析独立缓存，并最终组装为最终的配置对象。")]),a._v(" "),t("h2",{attrs:{id:"四、优化和调试"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#四、优化和调试"}},[a._v("#")]),a._v(" 四、优化和调试")]),a._v(" "),t("h3",{attrs:{id:"_1、可以打断点的关键方法"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#_1、可以打断点的关键方法"}},[a._v("#")]),a._v(" 1、可以打断点的关键方法")]),a._v(" "),t("h3",{attrs:{id:"_2、可以优化的地方"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#_2、可以优化的地方"}},[a._v("#")]),a._v(" 2、可以优化的地方")])])}),[],!1,null,null,null);t.default=e.exports}}]);
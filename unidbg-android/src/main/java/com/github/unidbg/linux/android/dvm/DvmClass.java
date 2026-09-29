package com.github.unidbg.linux.android.dvm;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.Symbol;
import com.github.unidbg.pointer.UnidbgPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class DvmClass extends DvmObject<Class<?>> {

    private static final Logger log = LoggerFactory.getLogger(DvmClass.class);

    private static final String ROOT_CLASS = "java/lang/Class";

    public final BaseVM vm;
    private final DvmClass superClass;
    private final DvmClass[] interfaceClasses;
    private final String className;

    protected DvmClass(BaseVM vm, String className, DvmClass superClass, DvmClass[] interfaceClasses) {
        this(vm, className, superClass, interfaceClasses, null);
    }

    protected DvmClass(BaseVM vm, String className, DvmClass superClass, DvmClass[] interfaceClasses, Class<?> value) {
        super(ROOT_CLASS.equals(className) ? null : vm.resolveClass(ROOT_CLASS), value);
        this.vm = vm;
        this.superClass = superClass;
        this.interfaceClasses = interfaceClasses;
        this.className = className;
    }

    @SuppressWarnings("unused")
    public DvmClass getSuperclass() {
        return superClass;
    }

    @SuppressWarnings("unused")
    public DvmClass[] getInterfaces() {
        return interfaceClasses;
    }

    @Override
    public DvmClass getObjectType() {
        if (ROOT_CLASS.equals(className)) {
            return this;
        }

        return super.getObjectType();
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return className.replace('/', '.');
    }

    public DvmObject<?> newObject(Object value) {
        return new DvmObject<>(this, value);
    }

    DvmObject<?> allocObject() {
        String signature = this.getClassName() + "->allocObject";
        if (log.isDebugEnabled()) {
            log.debug("allocObject signature={}", signature);
        }
        BaseVM vm = this.vm;
        return checkJni(vm, this).allocObject(vm, this, signature);
    }

    private final Map<Long, DvmMethod> staticMethodMap = new HashMap<>();
    private final Map<Integer, Long> methodStaticHashToId = new HashMap<>(4);

    final DvmMethod getStaticMethod(long id) {
        DvmMethod method = staticMethodMap.get(id);
        if (method == null && superClass != null) {
            method = superClass.getStaticMethod(id);
        }
        if (method == null) {
            for (DvmClass interfaceClass : interfaceClasses) {
                method = interfaceClass.getStaticMethod(id);
                if (method != null) {
                    break;
                }
            }
        }
        return method;
    }

    long getStaticMethodID(String methodName, String args) {
        String signature = getClassName() + "->" + methodName + args;
        int hash = vm.hash(signature);
        long id = methodStaticHashToId.computeIfAbsent(hash, integer -> vm.allocateMethodOrFieldSlot());
        if (log.isDebugEnabled()) {
            log.debug("getStaticMethodID signature={}, hash=0x{}, id=0x{}", signature, Long.toHexString(hash), Long.toHexString(id));
        }
        if (checkJni(vm, this).acceptMethod(this, signature, true)) {
            if (!staticMethodMap.containsKey(id)) {
                staticMethodMap.put(id, new DvmMethod(this, methodName, args, true));
            }
            return id;
        } else {
            return 0;
        }
    }

    private final Map<Long, DvmMethod> methodMap = new HashMap<>();
    private final Map<Integer, Long> methodHashToId = new HashMap<>(4);

    final DvmMethod getMethod(long id) {
        DvmMethod method = methodMap.get(id);
        if (method == null && superClass != null) {
            method = superClass.getMethod(id);
        }
        if (method == null) {
            for (DvmClass interfaceClass : interfaceClasses) {
                method = interfaceClass.getMethod(id);
                if (method != null) {
                    break;
                }
            }
        }
        return method;
    }

    long getMethodID(String methodName, String args) {
        String signature = getClassName() + "->" + methodName + args;
        int hash = vm.hash(signature);
        long id = methodHashToId.computeIfAbsent(hash, integer -> vm.allocateMethodOrFieldSlot());
        if (log.isDebugEnabled()) {
            log.debug("getMethodID signature={}, hash=0x{}, id=0x{}", signature, Long.toHexString(hash), Long.toHexString(id));
        }
        if (vm.jni == null || vm.jni.acceptMethod(this, signature, false)) {
            if (!methodMap.containsKey(id)) {
                methodMap.put(id, new DvmMethod(this, methodName, args, false));
            }
            return id;
        } else {
            return 0;
        }
    }

    private final Map<Long, DvmField> fieldMap = new HashMap<>();
    private final Map<Integer, Long> fieldHashToId = new HashMap<>(4);

    final DvmField getField(long id) {
        DvmField field = fieldMap.get(id);
        if (field == null && superClass != null) {
            field = superClass.getField(id);
        }
        if (field == null) {
            for (DvmClass interfaceClass : interfaceClasses) {
                field = interfaceClass.getField(id);
                if (field != null) {
                    break;
                }
            }
        }
        return field;
    }

    long getFieldID(String fieldName, String fieldType) {
        String signature = getClassName() + "->" + fieldName + ":" + fieldType;
        int hash = vm.hash(signature);
        long id = fieldHashToId.computeIfAbsent(hash, integer -> vm.allocateMethodOrFieldSlot());
        if (log.isDebugEnabled()) {
            log.debug("getFieldID signature={}, hash=0x{}, id=0x{}", signature, Long.toHexString(hash), Long.toHexString(id));
        }
        if (vm.jni == null || vm.jni.acceptField(this, signature, false)) {
            if (!fieldMap.containsKey(id)) {
                fieldMap.put(id, new DvmField(this, fieldName, fieldType, false));
            }
            return id;
        } else {
            return 0;
        }
    }

    private final Map<Long, DvmField> staticFieldMap = new HashMap<>();
    private final Map<Integer, Long> fieldStaticHashToId = new HashMap<>(4);

    final DvmField getStaticField(long id) {
        DvmField field = staticFieldMap.get(id);
        if (field == null && superClass != null) {
            field = superClass.getStaticField(id);
        }
        if (field == null) {
            for (DvmClass interfaceClass : interfaceClasses) {
                field = interfaceClass.getStaticField(id);
                if (field != null) {
                    break;
                }
            }
        }
        return field;
    }

    long getStaticFieldID(String fieldName, String fieldType) {
        String signature = getClassName() + "->" + fieldName + ":" + fieldType;
        int hash = vm.hash(signature);
        long id = fieldStaticHashToId.computeIfAbsent(hash, integer -> vm.allocateMethodOrFieldSlot());
        if (log.isDebugEnabled()) {
            log.debug("getStaticFieldID signature={}, hash=0x{}, id=0x{}", signature, Long.toHexString(hash), Long.toHexString(id));
        }
        if (vm.jni == null || vm.jni.acceptField(this, signature, true)) {
            if (!staticFieldMap.containsKey(id)) {
                staticFieldMap.put(id, new DvmField(this, fieldName, fieldType, true));
            }
            return id;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DvmClass dvmClass = (DvmClass) o;
        return Objects.equals(getClassName(), dvmClass.getClassName());
    }

    @Override
    public int hashCode() {
        return vm.hash(getClassName());
    }

    @Override
    public String toString() {
        return "class " + getClassName();
    }

    final Map<String, UnidbgPointer> nativesMap = new HashMap<>();

    public final UnidbgPointer findNativeFunction(Emulator<?> emulator, String method) {
        UnidbgPointer fnPtr = nativesMap.get(method);
        int index = method.indexOf('(');
        if (fnPtr == null && index == -1) {
            index = method.length();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Java_");
        mangleForJni(builder, getClassName());
        builder.append("_");
        mangleForJni(builder, method.substring(0, index));
        String symbolName = builder.toString();
        if (fnPtr == null) {
            for (Module module : emulator.getMemory().getLoadedModules()) {
                Symbol symbol = module.findSymbolByName(symbolName, false);
                if (symbol != null) {
                    fnPtr = (UnidbgPointer) symbol.createPointer(emulator);
                    break;
                }
            }
        }
        if (fnPtr == null) {
            throw new IllegalArgumentException("find method failed: " + method);
        }
        if (vm.verbose) {
            System.out.printf("Find native function %s => %s%n", symbolName, fnPtr);
        }
        return fnPtr;
    }

    private static void mangleForJni(StringBuilder builder, String name) {
        char[] chars = name.toCharArray();
        for (char c : chars) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
            } else if (c == '.' || c == '/') {
                builder.append("_");
            } else if (c == '_') {
                builder.append("_1");
            } else if (c == ';') {
                builder.append("_2");
            } else if (c == '[') {
                builder.append("_3");
            } else {
                builder.append(String.format("_0%04x", c & 0xffff));
            }
        }
    }
    
    public void callStaticJniMethod(Emulator<?> emulator, String method, Object...args) {
        try {
            callJniMethod(emulator, vm, this, this, method, args);
        } finally {
            vm.deleteLocalRefs();
        }
    }

    @SuppressWarnings("unused")
    public boolean callStaticJniMethodBoolean(Emulator<?> emulator, String method, Object...args) {
        return BaseVM.valueOf(callStaticJniMethodInt(emulator, method, args));
    }

    @SuppressWarnings("unused")
    public int callStaticJniMethodInt(Emulator<?> emulator, String method, Object...args) {
        try {
            return callJniMethod(emulator, vm, this, this, method, args).intValue();
        } finally {
            vm.deleteLocalRefs();
        }
    }

    @SuppressWarnings("unused")
    public long callStaticJniMethodLong(Emulator<?> emulator, String method, Object...args) {
        try {
            return callJniMethod(emulator, vm, this, this, method, args).longValue();
        } finally {
            vm.deleteLocalRefs();
        }
    }

    @SuppressWarnings("unused")
    public <T extends DvmObject<?>> T callStaticJniMethodObject(Emulator<?> emulator, String method, Object...args) {
        try {
            Number number = callJniMethod(emulator, vm, this, this, method, args);
            return vm.getObject(number.intValue());
        } finally {
            vm.deleteLocalRefs();
        }
    }

    final boolean isInstance(DvmClass dvmClass) {
        if (dvmClass == this) {
            return true;
        }

        for (DvmClass dc : interfaceClasses) {
            if (dc == dvmClass) {
                return true;
            }
        }
        if (superClass != null) {
            return superClass.isInstance(dvmClass);
        } else {
            return false;
        }
    }

    private JniFunction jni;

    protected final void setJni(JniFunction jni) {
        this.jni = jni;
    }

    final Jni getJni() {
        return jni;
    }

}

// 声明为仅客户端使用的类
@OnlyIn(Dist.CLIENT)
public class VertexFormat {
    // 定义未知元素的标识
    public static final int UNKNOWN_ELEMENT = -1;
    
    // 顶点格式元素列表（如位置、颜色、UV等）
    private final List<VertexFormatElement> elements;
    // 元素对应的属性名称列表
    private final List<String> names;
    // 单个顶点数据的总字节大小
    private final int vertexSize;
    // 元素存在的位掩码表示
    private final int elementsMask;
    // 各元素在顶点数据中的偏移量数组（按元素ID索引）
    private final int[] offsetsByElement = new int[32];
    // 立即模式绘制使用的顶点缓冲区
    @Nullable
    private VertexBuffer immediateDrawVertexBuffer;

    // 构造函数（包级访问权限）
    VertexFormat(List<VertexFormatElement> elements, List<String> names, IntList offsets, int vertexSize) {
        this.elements = elements;
        this.names = names;
        this.vertexSize = vertexSize;
        // 计算元素掩码（按位或组合所有元素的掩码）
        this.elementsMask = elements.stream().mapToInt(VertexFormatElement::mask).reduce(0, (a, b) -> a | b);

        // 初始化元素偏移量查找表
        for(int i = 0; i < this.offsetsByElement.length; ++i) {
            VertexFormatElement element = VertexFormatElement.byId(i);
            int index = element != null ? elements.indexOf(element) : -1;
            this.offsetsByElement[i] = index != -1 ? offsets.getInt(index) : -1;
        }
    }

    // 创建构建器的工厂方法
    public static Builder builder() {
        return new Builder();
    }

    // 格式化输出顶点格式信息
    public String toString() {
        StringBuilder sb = new StringBuilder("Vertex format (")
            .append(this.vertexSize).append(" bytes):\n");
        for(int i = 0; i < elements.size(); ++i) {
            VertexFormatElement element = elements.get(i);
            sb.append(i).append(". ")
              .append(names.get(i)).append(": ")
              .append(element).append(" @ ")
              .append(getOffset(element)).append('\n');
        }
        return sb.toString();
    }

    // 获取顶点字节大小
    public int getVertexSize() {
        return this.vertexSize;
    }

    // 获取元素列表
    public List<VertexFormatElement> getElements() {
        return this.elements;
    }

    // 检查是否包含某个元素
    public boolean contains(VertexFormatElement element) {
        return (this.elementsMask & element.mask()) != 0;
    }

    // 设置顶点缓冲状态（OpenGL属性指针）
    public void setupBufferState() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::_setupBufferState);
        } else {
            this._setupBufferState();
        }
    }

    private void _setupBufferState() {
        int stride = this.getVertexSize();
        for(int i = 0; i < elements.size(); ++i) {
            GlStateManager._enableVertexAttribArray(i); // 启用属性指针
            VertexFormatElement element = elements.get(i);
            element.setupBufferState(i, (long)getOffset(element), stride);
        }
    }

    // 内部构建器类
    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();
        private final IntList offsets = new IntArrayList();
        private int offset; // 当前累计偏移量

        Builder() {}

        // 添加元素并计算偏移
        public Builder add(String name, VertexFormatElement element) {
            elements.put(name, element);
            offsets.add(this.offset);
            this.offset += element.byteSize(); // 累加字节大小
            return this;
        }

        // 添加填充字节
        public Builder padding(int padding) {
            this.offset += padding;
            return this;
        }

        // 构建VertexFormat实例
        public VertexFormat build() {
            ImmutableMap<String, VertexFormatElement> elementMap = elements.buildOrThrow();
            return new VertexFormat(
                elementMap.values().asList(),
                elementMap.keySet().asList(),
                this.offsets,
                this.offset
            );
        }
    }

    // 图元渲染模式枚举
    @OnlyIn(Dist.CLIENT)
    public static enum Mode {
        LINES(4, 2, 2, false),         // GL_LINES
        LINE_STRIP(5, 2, 1, true),     // GL_LINE_STRIP
        DEBUG_LINES(1, 2, 2, false),   // 调试用线
        TRIANGLES(4, 3, 3, false),     // GL_TRIANGLES
        // ...其他模式

        public final int asGLMode;          // OpenGL模式常量
        public final int primitiveLength;   // 基本图元顶点数
        public final int primitiveStride;   // 图元间隔
        public final boolean connectedPrimitives; // 是否连续图元

        // 计算索引数量
        public int indexCount(int vertices) {
            switch (this) {
                case QUADS: return vertices / 4 * 6; // 四边形转三角形需要6个索引
                default: return vertices;
            }
        }
    }

    // 索引类型枚举
    @OnlyIn(Dist.CLIENT)
    public static enum IndexType {
        SHORT(5123, 2),  // GL_UNSIGNED_SHORT
        INT(5125, 4);     // GL_UNSIGNED_INT

        public final int asGLType;  // OpenGL类型常量
        public final int bytes;     // 字节大小

        // 根据索引数量自动选择类型
        public static IndexType least(int indexCount) {
            return (indexCount & 0xFFFF0000) != 0 ? INT : SHORT;
        }
    }
}

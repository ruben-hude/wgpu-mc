package dev.birb.wgpu.palette;

import dev.birb.wgpu.rust.WgpuNative;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PaletteResizeListener;

import java.lang.ref.Cleaner;
import java.nio.ByteOrder;
import java.util.List;
import java.util.function.Predicate;

public class RustPalette<T> implements Palette<T> {

    private final long rustPalettePointer;
    private final long rustIdList;
    private final IndexedIterable<T> idList;

    public static final Cleaner CLEANER = Cleaner.create();

    public RustPalette(long rustIdList, IndexedIterable<T> idList) {
        this(WgpuNative.createPalette(rustIdList), rustIdList, idList);
    }

    public RustPalette(long rustPalettePointer, long rustIdList, IndexedIterable<T> idList) {
        this.rustPalettePointer = rustPalettePointer;
        this.rustIdList = rustIdList;
        this.idList = idList;

        CLEANER.register(this, () -> WgpuNative.destroyPalette(rustPalettePointer));
    }

    @Override
    public int index(T object) {
        RustBlockStateAccessor accessor = (RustBlockStateAccessor) object;

        return WgpuNative.paletteIndex(this.rustPalettePointer, object, accessor.getRustBlockStateIndex());
    }

    @Override
    public boolean hasAny(Predicate<T> predicate) {
        return WgpuNative.paletteHasAny(this.rustPalettePointer, predicate);
    }

    @Override
    public T get(int id) {
        T t = (T) WgpuNative.paletteGet(this.rustPalettePointer, id);
        return t;
    }

    @Override
    public void readPacket(PacketByteBuf buf) {
        assert buf.nioBuffer().order() == ByteOrder.LITTLE_ENDIAN;

        WgpuNative.clearPalette(this.rustPalettePointer);

        int index = buf.readerIndex();

        int size = buf.readVarInt();

        int[] blockstateOffsets = new int[size];

        for(int i=0;i<size;i++) {
            T object = this.idList.get(buf.readVarInt());
            RustBlockStateAccessor accessor = (RustBlockStateAccessor) object;
            blockstateOffsets[i] = accessor.getRustBlockStateIndex();
        }

        int javaRead = buf.readerIndex();

        int wmRead = WgpuNative.paletteReadPacket(this.rustPalettePointer, buf.array(), index, blockstateOffsets);
        assert javaRead - index == wmRead;
    }

    @Override
    public void writePacket(PacketByteBuf buf) {
        int size = this.getSize();
        buf.writeVarInt(size);
        for (int i = 0; i < size; ++i) {
            buf.writeVarInt(this.idList.getRawId(this.get(i)));
        }
    }

    @Override
    public int getPacketSize() {
        int i = PacketByteBuf.getVarIntLength(this.getSize());
        for (int j = 0; j < this.getSize(); ++j) {
            i += PacketByteBuf.getVarIntLength(this.idList.getRawId(this.get(i)));
        }
        return i;
    }

    @Override
    public int getSize() {
        return WgpuNative.paletteSize(this.rustPalettePointer);
    }

    @Override
    public Palette<T> copy() {
        return new RustPalette<T>(WgpuNative.copyPalette(this.rustPalettePointer), this.rustIdList, this.idList);
    }

    public static <A> Palette<A> create(int bits, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> list) {
        return new RustPalette<A>(WgpuNative.uploadIdList((IndexedIterable<Object>) idList), idList);
    }

    public long getRustPointer() {
        return this.rustPalettePointer;
    }

}

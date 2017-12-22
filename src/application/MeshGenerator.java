package application;

import com.jogamp.opengl.GL2GL3;
import io.vavr.Tuple2;
import io.vavr.collection.Array;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import oglutils.OGLBuffers;
import oglutils.ToFloatArray;
import oglutils.ToIntArray;
import org.jetbrains.annotations.NotNull;
import transforms.Vec2D;

public class MeshGenerator {
    public static @NotNull OGLBuffers generateGrid(
            final @NotNull GL2GL3 gl,
            final int m, final int n,
            final @NotNull String shaderName) {
/*
        final List<Vec2D> vertices = new ArrayList<>();
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++) {
                vertices.add(new Vec2D(c / (n - 1.0), r / (m - 1.0)));
            }
*/
        Seq<Vec2D> vertices = Stream.range(0, m).flatMap(
                (final Integer r) -> Stream.range(0, n).map(
                        (final Integer c) ->
                                new Vec2D(c / (n - 1.0), r / (m - 1.0))
                )
        );

        Seq<Tuple2<Integer, Integer>> offsets =
                Array.of(0, 0, 1, 1, 0, 1).zip(Array.of(0, 1, 0, 0, 1, 1));
        Seq<Integer> indices = Stream.range(0, m - 1).flatMap(
                (final Integer r) -> Stream.range(0, n - 1).flatMap(
                        (final Integer c) -> offsets.map(
                                (final Tuple2<Integer, Integer> offset) ->
                                        (r + offset._1) * n + c + offset._2
                        )
                )
        );
/*
        List<Integer> indices = new ArrayList<>();
        for (int r = 0; r < m - 1; r++)
            for (int c = 0; c < n - 1; c++) {
                indices.add(r * n + c);
                indices.add(r * n + c + 1);
                indices.add((r + 1) * n + c);
                indices.add((r + 1) * n + c);
                indices.add(r * n + c + 1);
                indices.add((r + 1) * n + c + 1);
            }
*/


        final OGLBuffers.Attrib[] attributes = {
                new OGLBuffers.Attrib(shaderName, 2),
        };

        return new OGLBuffers(
                gl,
                ToFloatArray.convert(vertices.toJavaList()),
                attributes,
                ToIntArray.convert(indices.toJavaList()));
    }
}


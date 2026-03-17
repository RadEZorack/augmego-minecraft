package com.augmego.avatar.core;

import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AccessorByteData;
import de.javagl.jgltf.model.AccessorDatas;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorShortData;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AvatarLoader {
    public AvatarModel loadGlb(Path path) throws AvatarLoadException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return loadGlb(inputStream, path.getFileName().toString());
        } catch (IOException exception) {
            throw new AvatarLoadException("Failed to load GLB from path " + path, exception);
        }
    }

    public AvatarModel loadGlb(InputStream inputStream, String sourceName) throws AvatarLoadException {
        try {
            GltfModel gltfModel = new GltfModelReader().readWithoutReferences(inputStream);
            List<AvatarMesh> meshes = new ArrayList<>();

            for (MeshModel meshModel : gltfModel.getMeshModels()) {
                for (MeshPrimitiveModel primitiveModel : meshModel.getMeshPrimitiveModels()) {
                    meshes.add(readPrimitive(meshModel.getName(), primitiveModel));
                }
            }

            if (meshes.isEmpty()) {
                throw new AvatarLoadException("GLB contained no mesh primitives");
            }

            return new AvatarModel(sourceName, meshes);
        } catch (IOException exception) {
            throw new AvatarLoadException("Failed to parse GLB " + sourceName, exception);
        } catch (RuntimeException exception) {
            throw new AvatarLoadException("Invalid GLB " + sourceName, exception);
        }
    }

    private AvatarMesh readPrimitive(String meshName, MeshPrimitiveModel primitiveModel) throws AvatarLoadException {
        if (primitiveModel.getMode() != 4) {
            throw new AvatarLoadException("Only triangle mesh primitives are supported in this proof of concept");
        }

        Map<String, AccessorModel> attributes = primitiveModel.getAttributes();
        float[] positions = readFloatAccessor(attributes.get("POSITION"), 3);
        if (positions == null || positions.length == 0) {
            throw new AvatarLoadException("Mesh primitive is missing POSITION data");
        }

        float[] normals = readFloatAccessor(attributes.get("NORMAL"), 3);
        float[] uvs = readFloatAccessor(attributes.get("TEXCOORD_0"), 2);
        int[] indices = readIndices(primitiveModel.getIndices(), positions.length / 3);
        String materialName = primitiveModel.getMaterialModel() != null ? primitiveModel.getMaterialModel().getName() : meshName;
        return new AvatarMesh(positions, normals, uvs, indices, materialName);
    }

    private float[] readFloatAccessor(AccessorModel accessorModel, int components) {
        if (accessorModel == null) {
            return null;
        }

        AccessorFloatData accessorData = AccessorDatas.createFloat(accessorModel);
        int count = accessorData.getNumElements();
        float[] values = new float[count * accessorData.getNumComponentsPerElement()];
        for (int elementIndex = 0; elementIndex < count; elementIndex++) {
            for (int componentIndex = 0; componentIndex < components; componentIndex++) {
                values[elementIndex * components + componentIndex] = accessorData.get(elementIndex, componentIndex);
            }
        }
        return values;
    }

    private int[] readIndices(AccessorModel accessorModel, int vertexCount) {
        if (accessorModel == null) {
            int[] generated = new int[vertexCount];
            for (int index = 0; index < vertexCount; index++) {
                generated[index] = index;
            }
            return generated;
        }

        int[] values = new int[accessorModel.getCount()];
        if (accessorModel.getAccessorData() instanceof AccessorIntData intData) {
            for (int index = 0; index < accessorModel.getCount(); index++) {
                values[index] = intData.get(index);
            }
            return values;
        }
        if (accessorModel.getAccessorData() instanceof AccessorShortData shortData) {
            for (int index = 0; index < accessorModel.getCount(); index++) {
                values[index] = shortData.getInt(index);
            }
            return values;
        }
        if (accessorModel.getAccessorData() instanceof AccessorByteData byteData) {
            for (int index = 0; index < accessorModel.getCount(); index++) {
                values[index] = byteData.getInt(index);
            }
            return values;
        }

        throw new IllegalArgumentException("Unsupported index accessor type: " + accessorModel.getAccessorData().getClass().getName());
    }
}

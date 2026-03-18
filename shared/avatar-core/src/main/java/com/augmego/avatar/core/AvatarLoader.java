package com.augmego.avatar.core;

import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AccessorByteData;
import de.javagl.jgltf.model.AccessorDatas;
import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorIntData;
import de.javagl.jgltf.model.AccessorShortData;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
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
            Map<NodeModel, Integer> nodeIndices = createNodeIndexMap(gltfModel.getNodeModels());
            Map<SkinModel, Integer> skinIndices = createSkinIndexMap(gltfModel.getSkinModels());
            List<AvatarMesh> meshes = new ArrayList<>();
            List<AvatarNode> nodes = readNodes(gltfModel.getNodeModels(), nodeIndices);
            List<AvatarSkin> skins = readSkins(gltfModel.getSkinModels(), nodeIndices);
            List<AvatarAnimation> animations = readAnimations(gltfModel.getAnimationModels(), nodeIndices);

            for (NodeModel nodeModel : gltfModel.getNodeModels()) {
                List<MeshModel> nodeMeshes = nodeModel.getMeshModels();
                if (nodeMeshes == null || nodeMeshes.isEmpty()) {
                    continue;
                }

                int nodeIndex = nodeIndices.get(nodeModel);
                int skinIndex = nodeModel.getSkinModel() != null ? skinIndices.getOrDefault(nodeModel.getSkinModel(), -1) : -1;
                for (MeshModel meshModel : nodeMeshes) {
                    for (MeshPrimitiveModel primitiveModel : meshModel.getMeshPrimitiveModels()) {
                        meshes.add(readPrimitive(sourceName, meshModel.getName(), nodeIndex, skinIndex, primitiveModel));
                    }
                }
            }

            if (meshes.isEmpty()) {
                throw new AvatarLoadException("GLB contained no mesh primitives");
            }

            return new AvatarModel(sourceName, meshes, nodes, skins, animations);
        } catch (IOException exception) {
            throw new AvatarLoadException("Failed to parse GLB " + sourceName, exception);
        } catch (RuntimeException exception) {
            throw new AvatarLoadException("Invalid GLB " + sourceName, exception);
        }
    }

    private AvatarMesh readPrimitive(
        String sourceName,
        String meshName,
        int nodeIndex,
        int skinIndex,
        MeshPrimitiveModel primitiveModel
    ) throws AvatarLoadException {
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
        int[] jointIndices = readIntAccessor(attributes.get("JOINTS_0"), 4);
        float[] jointWeights = readFloatAccessor(attributes.get("WEIGHTS_0"), 4);
        int[] indices = readIndices(primitiveModel.getIndices(), positions.length / 3);
        String materialName = primitiveModel.getMaterialModel() != null ? primitiveModel.getMaterialModel().getName() : meshName;
        AvatarTexture texture = readBaseColorTexture(sourceName, materialName, primitiveModel.getMaterialModel());
        return new AvatarMesh(positions, normals, uvs, indices, nodeIndex, skinIndex, jointIndices, jointWeights, materialName, texture);
    }

    private AvatarTexture readBaseColorTexture(String sourceName, String materialName, MaterialModel materialModel) {
        if (!(materialModel instanceof MaterialModelV2 materialModelV2)) {
            return null;
        }

        TextureModel textureModel = materialModelV2.getBaseColorTexture();
        if (textureModel == null || textureModel.getImageModel() == null || textureModel.getImageModel().getImageData() == null) {
            return null;
        }

        ByteBuffer imageData = textureModel.getImageModel().getImageData().duplicate();
        byte[] bytes = new byte[imageData.remaining()];
        imageData.get(bytes);

        return new AvatarTexture(
            sourceName + ":" + (materialName != null ? materialName : "material"),
            bytes,
            textureModel.getImageModel().getMimeType()
        );
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

    private int[] readIntAccessor(AccessorModel accessorModel, int components) {
        if (accessorModel == null) {
            return null;
        }

        int count = accessorModel.getCount();
        int[] values = new int[count * components];
        if (accessorModel.getAccessorData() instanceof AccessorIntData intData) {
            for (int elementIndex = 0; elementIndex < count; elementIndex++) {
                for (int componentIndex = 0; componentIndex < components; componentIndex++) {
                    values[elementIndex * components + componentIndex] = intData.get(elementIndex, componentIndex);
                }
            }
            return values;
        }
        if (accessorModel.getAccessorData() instanceof AccessorShortData shortData) {
            for (int elementIndex = 0; elementIndex < count; elementIndex++) {
                for (int componentIndex = 0; componentIndex < components; componentIndex++) {
                    values[elementIndex * components + componentIndex] = shortData.getInt(elementIndex, componentIndex);
                }
            }
            return values;
        }
        if (accessorModel.getAccessorData() instanceof AccessorByteData byteData) {
            for (int elementIndex = 0; elementIndex < count; elementIndex++) {
                for (int componentIndex = 0; componentIndex < components; componentIndex++) {
                    values[elementIndex * components + componentIndex] = byteData.getInt(elementIndex, componentIndex);
                }
            }
            return values;
        }

        throw new IllegalArgumentException("Unsupported integer accessor type: " + accessorModel.getAccessorData().getClass().getName());
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

    private List<AvatarNode> readNodes(List<NodeModel> nodeModels, Map<NodeModel, Integer> nodeIndices) {
        List<AvatarNode> nodes = new ArrayList<>(nodeModels.size());
        for (NodeModel nodeModel : nodeModels) {
            int parentIndex = nodeModel.getParent() != null ? nodeIndices.getOrDefault(nodeModel.getParent(), -1) : -1;
            int[] children = new int[nodeModel.getChildren().size()];
            for (int childIndex = 0; childIndex < nodeModel.getChildren().size(); childIndex++) {
                children[childIndex] = nodeIndices.get(nodeModel.getChildren().get(childIndex));
            }

            nodes.add(new AvatarNode(
                nodeModel.getName(),
                parentIndex,
                children,
                defaultIfNull(nodeModel.getTranslation(), 0.0F, 0.0F, 0.0F),
                defaultIfNull(nodeModel.getRotation(), 0.0F, 0.0F, 0.0F, 1.0F),
                defaultIfNull(nodeModel.getScale(), 1.0F, 1.0F, 1.0F)
            ));
        }
        return nodes;
    }

    private List<AvatarSkin> readSkins(List<SkinModel> skinModels, Map<NodeModel, Integer> nodeIndices) {
        List<AvatarSkin> skins = new ArrayList<>(skinModels.size());
        for (SkinModel skinModel : skinModels) {
            int[] joints = new int[skinModel.getJoints().size()];
            for (int jointIndex = 0; jointIndex < skinModel.getJoints().size(); jointIndex++) {
                joints[jointIndex] = nodeIndices.get(skinModel.getJoints().get(jointIndex));
            }

            float[] inverseBindMatrices = new float[skinModel.getJoints().size() * 16];
            if (skinModel.getInverseBindMatrices() != null) {
                for (int jointIndex = 0; jointIndex < skinModel.getJoints().size(); jointIndex++) {
                    float[] jointMatrix = skinModel.getInverseBindMatrix(jointIndex, null);
                    System.arraycopy(jointMatrix, 0, inverseBindMatrices, jointIndex * 16, 16);
                }
            } else {
                for (int jointIndex = 0; jointIndex < skinModel.getJoints().size(); jointIndex++) {
                    inverseBindMatrices[jointIndex * 16] = 1.0F;
                    inverseBindMatrices[jointIndex * 16 + 5] = 1.0F;
                    inverseBindMatrices[jointIndex * 16 + 10] = 1.0F;
                    inverseBindMatrices[jointIndex * 16 + 15] = 1.0F;
                }
            }

            skins.add(new AvatarSkin(joints, inverseBindMatrices));
        }
        return skins;
    }

    private List<AvatarAnimation> readAnimations(List<AnimationModel> animationModels, Map<NodeModel, Integer> nodeIndices) {
        List<AvatarAnimation> animations = new ArrayList<>(animationModels.size());
        for (AnimationModel animationModel : animationModels) {
            List<AvatarAnimationChannel> channels = new ArrayList<>();
            float durationSeconds = 0.0F;

            for (AnimationModel.Channel channel : animationModel.getChannels()) {
                int nodeIndex = nodeIndices.getOrDefault(channel.getNodeModel(), -1);
                if (nodeIndex < 0 || channel.getPath() == null) {
                    continue;
                }

                float[] times = readAnimationInput(channel.getSampler().getInput());
                int components = componentsForPath(channel.getPath());
                if (components == 0) {
                    continue;
                }
                String interpolation = channel.getSampler().getInterpolation().toString();
                float[] values = readAnimationOutput(channel.getSampler().getOutput(), times.length, components, interpolation);
                if (times.length == 0 || values.length == 0) {
                    continue;
                }

                durationSeconds = Math.max(durationSeconds, times[times.length - 1]);
                channels.add(new AvatarAnimationChannel(
                    nodeIndex,
                    channel.getPath(),
                    times,
                    values,
                    components,
                    interpolation
                ));
            }

            if (!channels.isEmpty()) {
                animations.add(new AvatarAnimation(animationModel.getName(), durationSeconds, channels));
            }
        }
        return animations;
    }

    private float[] readAnimationInput(AccessorModel accessorModel) {
        if (accessorModel == null) {
            return new float[0];
        }
        return readFloatAccessor(accessorModel, 1);
    }

    private float[] readAnimationOutput(AccessorModel accessorModel, int keyframeCount, int components, String interpolation) {
        if (accessorModel == null || keyframeCount == 0) {
            return new float[0];
        }

        AccessorFloatData accessorData = AccessorDatas.createFloat(accessorModel);
        float[] values = new float[keyframeCount * components];
        int strideMultiplier = "CUBICSPLINE".equals(interpolation) ? 3 : 1;

        for (int keyframeIndex = 0; keyframeIndex < keyframeCount; keyframeIndex++) {
            int accessorElement = "CUBICSPLINE".equals(interpolation) ? keyframeIndex * strideMultiplier + 1 : keyframeIndex;
            for (int componentIndex = 0; componentIndex < components; componentIndex++) {
                values[keyframeIndex * components + componentIndex] = accessorData.get(accessorElement, componentIndex);
            }
        }

        return values;
    }

    private int componentsForPath(String path) {
        return switch (path) {
            case "rotation" -> 4;
            case "translation", "scale" -> 3;
            default -> 0;
        };
    }

    private Map<NodeModel, Integer> createNodeIndexMap(List<NodeModel> nodeModels) {
        Map<NodeModel, Integer> nodeIndices = new IdentityHashMap<>();
        for (int index = 0; index < nodeModels.size(); index++) {
            nodeIndices.put(nodeModels.get(index), index);
        }
        return nodeIndices;
    }

    private Map<SkinModel, Integer> createSkinIndexMap(List<SkinModel> skinModels) {
        Map<SkinModel, Integer> skinIndices = new IdentityHashMap<>();
        for (int index = 0; index < skinModels.size(); index++) {
            skinIndices.put(skinModels.get(index), index);
        }
        return skinIndices;
    }

    private float[] defaultIfNull(float[] values, float... fallback) {
        if (values == null) {
            return fallback;
        }
        return values.clone();
    }
}
